package com.ject.vs.notification.event;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationRepository;
import com.ject.vs.notification.domain.PushToken;
import com.ject.vs.notification.domain.PushTokenRepository;
import com.ject.vs.notification.port.out.FcmPayload;
import com.ject.vs.notification.port.out.PushSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationSenderTest {

    @InjectMocks
    private PushNotificationSender sender;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private PushSenderPort pushSender;

    @Mock
    private Clock clock;

    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-15T10:00:00Z");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
        lenient().when(clock.getZone()).thenReturn(ZONE_ID);
    }

    private Notification createNotificationWithId(Long id, Long userId) {
        Notification notification = Notification.ofVoteEnded(userId, 100L, "테스트 투표", "https://example.com/thumb.jpg", clock);
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    private PushToken createPushToken(Long userId, String token) {
        PushToken pushToken = mock(PushToken.class);
        given(pushToken.getUserId()).willReturn(userId);
        given(pushToken.getToken()).willReturn(token);
        return pushToken;
    }

    @Nested
    @DisplayName("on (이벤트 핸들러)")
    class OnEvent {

        @Test
        @DisplayName("빈 이벤트는 아무 작업도 하지 않는다")
        void does_nothing_for_empty_event() {
            sender.on(new NotificationCreatedEvent(List.of()));

            verify(notificationRepository, never()).findAllById(anyList());
        }

        @Test
        @DisplayName("알림이 존재하지 않으면 아무 작업도 하지 않는다")
        void does_nothing_when_notifications_not_found() {
            given(notificationRepository.findAllById(List.of(1L))).willReturn(List.of());

            sender.on(new NotificationCreatedEvent(List.of(1L)));

            verify(pushTokenRepository, never()).findAllByUserIdIn(anyList());
        }

        @Test
        @DisplayName("토큰이 없으면 FCM 발송 없이 sent 처리만 한다")
        void marks_sent_without_fcm_when_no_tokens() {
            Notification notification = createNotificationWithId(1L, 1L);
            given(notificationRepository.findAllById(List.of(1L))).willReturn(List.of(notification));
            given(pushTokenRepository.findAllByUserIdIn(List.of(1L))).willReturn(List.of());

            sender.on(new NotificationCreatedEvent(List.of(1L)));

            assertThat(notification.isSent()).isTrue();
            verify(pushSender, never()).sendMulticast(anyList(), any(FcmPayload.class));
        }

        @Test
        @DisplayName("토큰이 있으면 FCM을 발송하고 sent 처리한다")
        void sends_fcm_and_marks_sent() {
            Notification notification = createNotificationWithId(1L, 1L);
            PushToken pushToken = createPushToken(1L, "token-123");

            given(notificationRepository.findAllById(List.of(1L))).willReturn(List.of(notification));
            given(pushTokenRepository.findAllByUserIdIn(List.of(1L))).willReturn(List.of(pushToken));
            given(pushSender.sendMulticast(anyList(), any(FcmPayload.class))).willReturn(List.of());

            sender.on(new NotificationCreatedEvent(List.of(1L)));

            assertThat(notification.isSent()).isTrue();
            verify(pushSender).sendMulticast(eq(List.of("token-123")), any(FcmPayload.class));
        }

        @Test
        @DisplayName("FCM 페이로드가 올바르게 생성된다")
        void creates_correct_fcm_payload() {
            Notification notification = createNotificationWithId(1L, 1L);
            PushToken pushToken = createPushToken(1L, "token-123");

            given(notificationRepository.findAllById(List.of(1L))).willReturn(List.of(notification));
            given(pushTokenRepository.findAllByUserIdIn(List.of(1L))).willReturn(List.of(pushToken));
            given(pushSender.sendMulticast(anyList(), any(FcmPayload.class))).willReturn(List.of());

            sender.on(new NotificationCreatedEvent(List.of(1L)));

            ArgumentCaptor<FcmPayload> payloadCaptor = ArgumentCaptor.forClass(FcmPayload.class);
            verify(pushSender).sendMulticast(anyList(), payloadCaptor.capture());

            FcmPayload payload = payloadCaptor.getValue();
            assertThat(payload.notificationId()).isEqualTo(1L);
            assertThat(payload.voteId()).isEqualTo(100L);
            assertThat(payload.title()).isEqualTo("투표 결과가 공개됐어요");
            assertThat(payload.thumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
        }

        @Test
        @DisplayName("만료된 토큰을 삭제한다")
        void deletes_expired_tokens() {
            Notification notification = createNotificationWithId(1L, 1L);
            PushToken pushToken = createPushToken(1L, "token-123");

            given(notificationRepository.findAllById(List.of(1L))).willReturn(List.of(notification));
            given(pushTokenRepository.findAllByUserIdIn(List.of(1L))).willReturn(List.of(pushToken));
            given(pushSender.sendMulticast(anyList(), any(FcmPayload.class)))
                    .willReturn(List.of("expired-token-1", "expired-token-2"));

            sender.on(new NotificationCreatedEvent(List.of(1L)));

            verify(pushTokenRepository).deleteAllByTokenIn(List.of("expired-token-1", "expired-token-2"));
        }

        @Test
        @DisplayName("만료된 토큰이 없으면 삭제하지 않는다")
        void does_not_delete_when_no_expired_tokens() {
            Notification notification = createNotificationWithId(1L, 1L);
            PushToken pushToken = createPushToken(1L, "token-123");

            given(notificationRepository.findAllById(List.of(1L))).willReturn(List.of(notification));
            given(pushTokenRepository.findAllByUserIdIn(List.of(1L))).willReturn(List.of(pushToken));
            given(pushSender.sendMulticast(anyList(), any(FcmPayload.class))).willReturn(List.of());

            sender.on(new NotificationCreatedEvent(List.of(1L)));

            verify(pushTokenRepository, never()).deleteAllByTokenIn(anyList());
        }

        @Test
        @DisplayName("여러 사용자에게 알림을 발송한다")
        void sends_to_multiple_users() {
            Notification notification1 = createNotificationWithId(1L, 1L);
            Notification notification2 = createNotificationWithId(2L, 2L);
            PushToken pushToken1 = createPushToken(1L, "token-1");
            PushToken pushToken2 = createPushToken(2L, "token-2");

            given(notificationRepository.findAllById(List.of(1L, 2L)))
                    .willReturn(List.of(notification1, notification2));
            given(pushTokenRepository.findAllByUserIdIn(anyList()))
                    .willReturn(List.of(pushToken1, pushToken2));
            given(pushSender.sendMulticast(anyList(), any(FcmPayload.class))).willReturn(List.of());

            sender.on(new NotificationCreatedEvent(List.of(1L, 2L)));

            assertThat(notification1.isSent()).isTrue();
            assertThat(notification2.isSent()).isTrue();
            verify(pushSender, times(2)).sendMulticast(anyList(), any(FcmPayload.class));
        }

        @Test
        @DisplayName("토큰이 없는 사용자도 sent 처리된다")
        void marks_sent_for_users_without_tokens() {
            Notification notification1 = createNotificationWithId(1L, 1L);
            Notification notification2 = createNotificationWithId(2L, 2L);
            PushToken pushToken1 = createPushToken(1L, "token-1");
            // user 2는 토큰 없음

            given(notificationRepository.findAllById(List.of(1L, 2L)))
                    .willReturn(List.of(notification1, notification2));
            given(pushTokenRepository.findAllByUserIdIn(anyList()))
                    .willReturn(List.of(pushToken1));
            given(pushSender.sendMulticast(anyList(), any(FcmPayload.class))).willReturn(List.of());

            sender.on(new NotificationCreatedEvent(List.of(1L, 2L)));

            assertThat(notification1.isSent()).isTrue();
            assertThat(notification2.isSent()).isTrue();
            verify(pushSender, times(1)).sendMulticast(anyList(), any(FcmPayload.class));
        }
    }
}
