package com.ject.vs.analytics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 행동 로그 한 건을 표현하는 값 객체. 이벤트 이름과 이벤트별 로그 변수를 담는다.
 *
 * <p>공통 로그 변수(user_id / anonymous_id / is_member / platform / occurred_at)는
 * {@link AnalyticsEventLogger#log(AnalyticsEvent)} 시점에 채워지므로 여기서는 다루지 않는다.
 *
 * <p>로거에 의존하지 않는 순수 값 객체이므로, 로거를 목(mock)으로 주입한 테스트에서도
 * {@code log(...)} 가 no-op이 되어 NPE 없이 안전하다.
 *
 * <pre>{@code
 * analytics.log(AnalyticsEvent.of("vote_detail_viewed")
 *         .put("vote_id", voteId)
 *         .put("vote_status", status));
 * }</pre>
 */
public final class AnalyticsEvent {

    private final String name;
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private Long userId;
    private boolean userIdOverridden;
    private String anonymousId;
    private boolean anonymousIdOverridden;

    private AnalyticsEvent(String name) {
        this.name = name;
    }

    public static AnalyticsEvent of(String name) {
        return new AnalyticsEvent(name);
    }

    /** 이벤트별 로그 변수 추가. null 값도 그대로 기록한다. */
    public AnalyticsEvent put(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    /** SecurityContext에서 user_id를 얻을 수 없는 흐름(예: OAuth 로그인 성공 핸들러)에서 직접 지정. */
    public AnalyticsEvent userId(Long userId) {
        this.userId = userId;
        this.userIdOverridden = true;
        return this;
    }

    /** 컨트롤러가 이미 보유한 anonymous_id를 직접 지정(쿠키 재파싱 생략). */
    public AnalyticsEvent anonymousId(String anonymousId) {
        this.anonymousId = anonymousId;
        this.anonymousIdOverridden = true;
        return this;
    }

    String name() {
        return name;
    }

    Map<String, Object> properties() {
        return properties;
    }

    boolean userIdOverridden() {
        return userIdOverridden;
    }

    Long userId() {
        return userId;
    }

    boolean anonymousIdOverridden() {
        return anonymousIdOverridden;
    }

    String anonymousId() {
        return anonymousId;
    }
}
