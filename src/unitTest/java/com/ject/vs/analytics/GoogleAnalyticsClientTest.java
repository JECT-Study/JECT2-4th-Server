package com.ject.vs.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GA4 Measurement Protocol 페이로드 변환 규칙 검증(전송 없이 순수 변환만).
 */
class GoogleAnalyticsClientTest {

    private GoogleAnalyticsClient client(boolean enabled) {
        GoogleAnalyticsProperties props =
                new GoogleAnalyticsProperties(enabled, "G-TEST", "secret", null);
        return new GoogleAnalyticsClient(props, RestClient.builder());
    }

    @Test
    void 비회원은_anonymous_id를_client_id로_쓰고_user_id는_빠진다() {
        Map<String, Object> payload = client(true).buildPayload(
                "landing_visited", null, "anon-uuid", false, "web", Map.of());

        assertThat(payload.get("client_id")).isEqualTo("anon-uuid");
        assertThat(payload).doesNotContainKey("user_id");
    }

    @Test
    void 회원은_user_id가_문자열로_들어가고_익명쿠키가_없으면_uid로_client_id를_만든다() {
        Map<String, Object> payload = client(true).buildPayload(
                "signup_completed", 42L, null, true, "web", Map.of());

        assertThat(payload.get("user_id")).isEqualTo("42");
        assertThat(payload.get("client_id")).isEqualTo("uid.42");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 공통_파라미터와_이벤트_속성이_params에_합쳐진다() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("vote_id", 123);
        props.put("vote_status", "ONGOING");

        Map<String, Object> payload = client(true).buildPayload(
                "vote_detail_viewed", 42L, "anon", true, "ios", props);

        Map<String, Object> event = ((List<Map<String, Object>>) payload.get("events")).get(0);
        assertThat(event.get("name")).isEqualTo("vote_detail_viewed");

        Map<String, Object> params = (Map<String, Object>) event.get("params");
        assertThat(params)
                .containsEntry("engagement_time_msec", 1)
                .containsEntry("is_member", true)
                .containsEntry("platform", "ios")
                .containsEntry("vote_id", 123)
                .containsEntry("vote_status", "ONGOING");
    }

    @Test
    @SuppressWarnings("unchecked")
    void null_속성은_제외되고_긴_문자열은_100자로_잘린다() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("nullable", null);
        props.put("long_text", "x".repeat(150));

        Map<String, Object> payload = client(true).buildPayload(
                "some_event", 1L, "anon", true, "web", props);

        Map<String, Object> event = ((List<Map<String, Object>>) payload.get("events")).get(0);
        Map<String, Object> params = (Map<String, Object>) event.get("params");

        assertThat(params).doesNotContainKey("nullable");
        assertThat((String) params.get("long_text")).hasSize(100);
    }
}
