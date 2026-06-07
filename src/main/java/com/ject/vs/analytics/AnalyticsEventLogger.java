package com.ject.vs.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 행동 로그(analytics event)를 구조화된 JSON 한 줄로 남기는 컴포넌트.
 *
 * <p>공통 로그 변수(user_id / anonymous_id / is_member / platform / occurred_at)는
 * 현재 HTTP 요청 컨텍스트에서 자동으로 채워진다. 이벤트별 변수는 {@link AnalyticsEvent}로 전달한다.
 *
 * <pre>{@code
 * analytics.log(AnalyticsEvent.of("vote_detail_viewed")
 *         .put("vote_id", voteId)
 *         .put("vote_status", status));
 * }</pre>
 *
 * <p>유일한 진입점이 void {@code log(AnalyticsEvent)}이므로 테스트에서 목(mock)으로 주입해도
 * 부수효과 없이 동작한다. 로그 적재 실패가 비즈니스 로직에 영향을 주지 않도록 모든 예외를 삼킨다.
 */
@Component
@RequiredArgsConstructor
public class AnalyticsEventLogger {

    /** 별도 로거 이름으로 분리하여 수집/필터링이 쉽도록 한다. */
    private static final Logger log = LoggerFactory.getLogger("analytics");

    private static final String ANONYMOUS_COOKIE = "anonymous_id";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public void log(AnalyticsEvent event) {
        try {
            HttpServletRequest request = currentRequest();

            Long userId = event.userIdOverridden() ? event.userId() : resolveUserId();
            String anonymousId = event.anonymousIdOverridden()
                    ? event.anonymousId()
                    : resolveAnonymousId(request);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", event.name());
            payload.put("user_id", userId);
            payload.put("anonymous_id", anonymousId);
            payload.put("is_member", userId != null);
            payload.put("platform", resolvePlatform(request));
            payload.put("occurred_at", Instant.now(clock).toString());
            payload.putAll(event.properties());

            AnalyticsEventLogger.log.info(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            // 로깅 실패가 요청 처리에 영향을 주지 않도록 흡수
            AnalyticsEventLogger.log.warn("analytics event logging failed for '{}': {}", event.name(), e.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    private Long resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        return (principal instanceof Long userId) ? userId : null;
    }

    private String resolveAnonymousId(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> ANONYMOUS_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /** User-Agent 기반 플랫폼 분류: ios / android / web. */
    private String resolvePlatform(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) return "unknown";
        String lower = ua.toLowerCase();
        if (lower.contains("android")) return "android";
        if (lower.contains("iphone") || lower.contains("ipad") || lower.contains("ios")
                || lower.contains("cfnetwork") || lower.contains("darwin")) {
            return "ios";
        }
        return "web";
    }
}
