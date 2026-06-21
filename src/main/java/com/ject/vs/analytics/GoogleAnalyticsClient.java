package com.ject.vs.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 행동 로그를 GA4(Google Analytics 4)로도 전송하는 어댑터.
 *
 * <p>RDB(analytics_events) 적재와 별개로, PM·디자이너가 GA4 대시보드에서 같은 이벤트를
 * 실시간으로 볼 수 있게 한다. 서버 사이드라 SDK가 아니라
 * <a href="https://developers.google.com/analytics/devguides/collection/protocol/ga4">GA4 Measurement Protocol</a>
 * (HTTP POST)로 전송한다.
 *
 * <p>설계 원칙(기존 {@link AnalyticsEventLogger}와 동일):
 * <ul>
 *   <li><b>비즈니스에 영향 없음</b> — 모든 예외를 삼키고, 미설정 시 no-op.</li>
 *   <li><b>요청 지연 없음</b> — 외부 HTTP라 {@code @Async}로 분리(fire-and-forget).
 *       호출 스레드에서 필요한 값(clientId/userId/params)을 모두 인자로 받으므로
 *       비동기 스레드에 요청 컨텍스트가 없어도 안전하다.</li>
 * </ul>
 */
@Component
public class GoogleAnalyticsClient {

    /** 전송 실패 경고 등 내부 진단용 로거(파일 어펜더는 "analytics" 로거명 공유). */
    private static final Logger log = LoggerFactory.getLogger("analytics");

    /** GA4가 사용자를 '참여(engaged)'로 집계하도록 넣는 최소 참여 시간(ms). */
    private static final int ENGAGEMENT_TIME_MSEC = 1;

    /** GA4 이벤트 파라미터 값 문자열 최대 길이(MP 제약: 100자). */
    private static final int MAX_VALUE_LENGTH = 100;

    private final GoogleAnalyticsProperties properties;
    private final RestClient restClient;

    public GoogleAnalyticsClient(GoogleAnalyticsProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        // 외부망 지연이 비동기 스레드를 오래 점유하지 않도록 짧은 타임아웃을 둔다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(2).toMillis());
        this.restClient = builder.requestFactory(factory).build();
    }

    @Async("analyticsExecutor")
    public void send(String eventName, Long userId, String anonymousId, boolean member,
                     String platform, Map<String, Object> properties) {
        if (!this.properties.isConfigured()) {
            return;
        }
        try {
            Map<String, Object> payload = buildPayload(eventName, userId, anonymousId, member, platform, properties);

            String url = UriComponentsBuilder.fromUriString(this.properties.endpoint())
                    .queryParam("measurement_id", this.properties.measurementId())
                    .queryParam("api_secret", this.properties.apiSecret())
                    .build()
                    .toUriString();

            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("GA4 forwarding failed for '{}': {}", eventName, e.getMessage());
        }
    }

    /**
     * GA4 Measurement Protocol 요청 본문을 만든다. (전송 없이 순수 변환 — 테스트 가능)
     *
     * <pre>{@code
     * { "client_id": "...", "user_id": "42", "events": [ { "name": "...", "params": { ... } } ] }
     * }</pre>
     */
    Map<String, Object> buildPayload(String eventName, Long userId, String anonymousId, boolean member,
                                     String platform, Map<String, Object> properties) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("name", eventName);
        event.put("params", buildParams(member, platform, properties));

        Map<String, Object> payload = new LinkedHashMap<>();
        // client_id는 필수. 비회원 쿠키(anonymous_id)를 우선 사용해 가입 전후 흐름을 한 사용자로 잇는다.
        payload.put("client_id", resolveClientId(anonymousId, userId));
        if (userId != null) {
            payload.put("user_id", String.valueOf(userId));
        }
        payload.put("events", List.of(event));
        return payload;
    }

    private Map<String, Object> buildParams(boolean member, String platform, Map<String, Object> properties) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("engagement_time_msec", ENGAGEMENT_TIME_MSEC);
        params.put("is_member", member);
        if (platform != null) {
            params.put("platform", platform);
        }
        if (properties != null) {
            properties.forEach((key, value) -> {
                Object sanitized = sanitize(value);
                if (sanitized != null) {
                    params.put(key, sanitized);
                }
            });
        }
        return params;
    }

    /** GA4 파라미터 값 규칙에 맞춘다: null은 제외, 숫자는 그대로, 그 외는 문자열(최대 100자)로. */
    private Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return value;
        }
        String text = value.toString();
        return text.length() > MAX_VALUE_LENGTH ? text.substring(0, MAX_VALUE_LENGTH) : text;
    }

    /** 익명 쿠키 → 회원 id → 랜덤 UUID 순으로 안정적인 client_id를 고른다. */
    private String resolveClientId(String anonymousId, Long userId) {
        if (anonymousId != null && !anonymousId.isBlank()) {
            return anonymousId;
        }
        if (userId != null) {
            return "uid." + userId;
        }
        return UUID.randomUUID().toString();
    }
}
