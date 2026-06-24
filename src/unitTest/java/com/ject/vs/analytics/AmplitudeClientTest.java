package com.ject.vs.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Amplitude HTTP V2 페이로드 변환 규칙 검증(전송 없이 순수 변환만).
 */
class AmplitudeClientTest {

    private AmplitudeClient client() {
        AmplitudeProperties props = new AmplitudeProperties(true, "test-key", null);
        return new AmplitudeClient(props, RestClient.builder());
    }

    @Test
    @SuppressWarnings("unchecked")
    void api_key와_event_type가_담긴다() {
        Map<String, Object> payload = client().buildPayload(
                "landing_visited", null, "anon-uuid", false, "web", Map.of());

        assertThat(payload.get("api_key")).isEqualTo("test-key");
        Map<String, Object> event = ((List<Map<String, Object>>) payload.get("events")).get(0);
        assertThat(event.get("event_type")).isEqualTo("landing_visited");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 비회원은_익명쿠키를_device_id로_쓰고_user_id는_빠진다() {
        Map<String, Object> event = firstEvent(client().buildPayload(
                "landing_visited", null, "anon-uuid", false, "web", Map.of()));

        assertThat(event).doesNotContainKey("user_id");
        assertThat(event.get("device_id")).isEqualTo("anon-uuid");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 회원은_uid_접두사_user_id를_쓰고_익명쿠키가_없으면_device_id는_생략된다() {
        Map<String, Object> event = firstEvent(client().buildPayload(
                "signup_completed", 42L, null, true, "web", Map.of()));

        assertThat(event.get("user_id")).isEqualTo("uid.42");
        assertThat(event).doesNotContainKey("device_id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 익명쿠키와_user_id가_모두_있으면_둘_다_실어_identity를_잇는다() {
        Map<String, Object> event = firstEvent(client().buildPayload(
                "signup_completed", 42L, "anon-uuid", true, "web", Map.of()));

        assertThat(event.get("user_id")).isEqualTo("uid.42");
        assertThat(event.get("device_id")).isEqualTo("anon-uuid");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 비회원이고_익명쿠키도_없으면_5자_이상의_랜덤_device_id가_생성된다() {
        Map<String, Object> event = firstEvent(client().buildPayload(
                "landing_visited", null, null, false, "web", Map.of()));

        assertThat(event).doesNotContainKey("user_id");
        assertThat((String) event.get("device_id")).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void is_member와_이벤트_속성이_event_properties에_합쳐진다() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("vote_id", 123);
        props.put("vote_status", "ONGOING");

        Map<String, Object> event = firstEvent(client().buildPayload(
                "vote_detail_viewed", 42L, "anon", true, "ios", props));

        assertThat(event.get("platform")).isEqualTo("ios");
        Map<String, Object> eventProperties = (Map<String, Object>) event.get("event_properties");
        assertThat(eventProperties)
                .containsEntry("is_member", true)
                .containsEntry("vote_id", 123)
                .containsEntry("vote_status", "ONGOING");
    }

    @Test
    @SuppressWarnings("unchecked")
    void null_속성은_event_properties에서_제외된다() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("nullable", null);
        props.put("kept", "value");

        Map<String, Object> event = firstEvent(client().buildPayload(
                "some_event", 1L, "anon", true, "web", props));

        Map<String, Object> eventProperties = (Map<String, Object>) event.get("event_properties");
        assertThat(eventProperties).doesNotContainKey("nullable").containsEntry("kept", "value");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstEvent(Map<String, Object> payload) {
        return ((List<Map<String, Object>>) payload.get("events")).get(0);
    }
}
