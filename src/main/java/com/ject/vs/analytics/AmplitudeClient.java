package com.ject.vs.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 행동 로그를 Amplitude로도 전송하는 어댑터.
 *
 * <p>RDB(analytics_events) 적재·GA4 전송과 별개로, PM·디자이너가 Amplitude 대시보드에서
 * 같은 이벤트(퍼널/리텐션 등)를 볼 수 있게 한다. 서버 사이드라 SDK가 아니라
 * <a href="https://amplitude.com/docs/apis/analytics/http-v2">HTTP V2 API</a>(HTTP POST)로 전송한다.
 *
 * <p>설계 원칙({@link GoogleAnalyticsClient}와 동일):
 * <ul>
 *   <li><b>비즈니스에 영향 없음</b> — 모든 예외를 삼키고, 미설정 시 no-op.</li>
 *   <li><b>요청 지연 없음</b> — 외부 HTTP라 {@code @Async}로 분리(fire-and-forget).
 *       호출 스레드에서 필요한 값을 모두 인자로 받으므로 비동기 스레드에 요청 컨텍스트가 없어도 안전하다.</li>
 * </ul>
 */
@Component
public class AmplitudeClient {

    /** 전송 실패 경고 등 내부 진단용 로거(파일 어펜더는 "analytics" 로거명 공유). */
    private static final Logger log = LoggerFactory.getLogger("analytics");

    /** Amplitude는 user_id / device_id가 최소 5자 이상이어야 이벤트를 수락한다. */
    private static final int MIN_ID_LENGTH = 5;

    private final AmplitudeProperties properties;
    private final RestClient restClient;

    public AmplitudeClient(AmplitudeProperties properties, RestClient.Builder builder) {
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

            restClient.post()
                    .uri(this.properties.endpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Amplitude forwarding failed for '{}': {}", eventName, e.getMessage());
        }
    }

    /**
     * Amplitude HTTP V2 요청 본문을 만든다. (전송 없이 순수 변환 — 테스트 가능)
     *
     * <pre>{@code
     * { "api_key": "...", "events": [ { "event_type": "...", "user_id": "...", "device_id": "...", ... } ] }
     * }</pre>
     */
    Map<String, Object> buildPayload(String eventName, Long userId, String anonymousId, boolean member,
                                     String platform, Map<String, Object> properties) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event_type", eventName);

        // 회원이면 안정적인 user_id를, 비회원 쿠키가 있으면 device_id를 같이 실어
        // 가입 전후 흐름을 한 사용자로 잇는다(Amplitude의 identity 병합).
        if (userId != null) {
            event.put("user_id", "uid." + userId);
        }
        String deviceId = resolveDeviceId(anonymousId, userId);
        if (deviceId != null) {
            event.put("device_id", deviceId);
        }
        if (platform != null) {
            event.put("platform", platform);
        }
        event.put("event_properties", buildEventProperties(member, properties));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("api_key", this.properties.apiKey());
        payload.put("events", List.of(event));
        return payload;
    }

    private Map<String, Object> buildEventProperties(boolean member, Map<String, Object> properties) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("is_member", member);
        if (properties != null) {
            properties.forEach((key, value) -> {
                Object sanitized = sanitize(value);
                if (sanitized != null) {
                    result.put(key, sanitized);
                }
            });
        }
        return result;
    }

    /** null은 제외, 숫자/불리언은 그대로, 그 외는 문자열로. */
    private Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return value.toString();
    }

    /**
     * device_id를 정한다: 익명 쿠키(5자 이상) → 회원 id 파생값 → 랜덤 UUID 순.
     * Amplitude는 user_id/device_id 중 하나가 5자 이상이어야 하므로, 둘 다 비는 경우를 막는다.
     */
    private String resolveDeviceId(String anonymousId, Long userId) {
        if (anonymousId != null && anonymousId.length() >= MIN_ID_LENGTH) {
            return anonymousId;
        }
        if (userId != null) {
            // 회원은 user_id("uid.N")가 이미 5자 이상이므로 device_id는 굳이 채우지 않는다.
            return null;
        }
        return UUID.randomUUID().toString();
    }
}
