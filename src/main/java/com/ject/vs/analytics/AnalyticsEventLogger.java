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

/**
 * 행동 로그(analytics event)를 RDB(analytics_events 테이블)에 한 건씩 적재하는 컴포넌트.
 *
 * <p>공통 로그 변수(user_id / anonymous_id / is_member / platform / occurred_at)는
 * 현재 HTTP 요청 컨텍스트에서 자동으로 채워지고, 이벤트별 가변 변수는 JSON 문자열로
 * 직렬화해 properties 컬럼에 담는다. 이벤트별 변수는 {@link AnalyticsEvent}로 전달한다.
 *
 * <pre>{@code
 * analytics.log(AnalyticsEvent.of("vote_detail_viewed")
 *         .put("vote_id", voteId)
 *         .put("vote_status", status));
 * }</pre>
 *
 * <p>유일한 진입점이 void {@code log(AnalyticsEvent)}이므로 테스트에서 목(mock)으로 주입해도
 * 부수효과 없이 동작한다. 적재 실패가 비즈니스 로직에 영향을 주지 않도록 모든 예외를 삼킨다.
 */
@Component
@RequiredArgsConstructor
public class AnalyticsEventLogger {

    /** 적재 실패 경고 등 내부 진단용 로거. */
    private static final Logger log = LoggerFactory.getLogger("analytics");

    private static final String ANONYMOUS_COOKIE = "anonymous_id";

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AnalyticsEventRepository analyticsEventRepository;

    public void log(AnalyticsEvent event) {
        try {
            HttpServletRequest request = currentRequest();

            Long userId = event.userIdOverridden() ? event.userId() : resolveUserId();
            String anonymousId = event.anonymousIdOverridden()
                    ? event.anonymousId()
                    : resolveAnonymousId(request);

            // 이벤트별 가변 속성은 JSON 문자열로 직렬화해 properties 컬럼에 담는다.
            String properties = event.properties().isEmpty()
                    ? null
                    : objectMapper.writeValueAsString(event.properties());

            analyticsEventRepository.save(new AnalyticsEventRecord(
                    event.name(),
                    userId,
                    anonymousId,
                    userId != null,
                    resolvePlatform(request),
                    Instant.now(clock),
                    properties));
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
