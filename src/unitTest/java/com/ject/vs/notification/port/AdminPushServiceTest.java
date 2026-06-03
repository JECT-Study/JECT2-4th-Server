package com.ject.vs.notification.port;

import com.ject.vs.config.AdminProperties;
import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationRepository;
import com.ject.vs.notification.event.NotificationCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminPushServiceTest {

    @InjectMocks
    private AdminPushService service;

    @Mock
    private AdminProperties adminProperties;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    private static final Long ADMIN_USER_ID = 1L;
    private static final Long NORMAL_USER_ID = 2L;
    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-15T10:00:00Z");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setUp() {
        lenient().when(adminProperties.userIds()).thenReturn(List.of(ADMIN_USER_ID));
        lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
        lenient().when(clock.getZone()).thenReturn(ZONE_ID);
    }

    @Nested
    @DisplayName("관리자 권한 검증")
    class AdminValidation {

        @Test
        @DisplayName("관리자 사용자는 테스트 푸시를 발송할 수 있다")
        void admin_can_send_test_push() {
            Notification savedNotification = Notification.ofCustom(
                    6L, 100L, "테스트 알림", "테스트 본문", null, clock);
            ReflectionTestUtils.setField(savedNotification, "id", 1L);

            given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

            Long result = service.sendTestPush(ADMIN_USER_ID, 6L, "테스트 알림", "테스트 본문", 100L, null);

            assertThat(result).isEqualTo(1L);
            verify(notificationRepository).save(any(Notification.class));
            verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
        }

        @Test
        @DisplayName("일반 사용자는 테스트 푸시 발송이 불가능하다")
        void non_admin_cannot_send_test_push() {
            assertThatThrownBy(() ->
                    service.sendTestPush(NORMAL_USER_ID, 6L, "테스트", "테스트", null, null)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("관리자 권한이 없습니다");
        }

        @Test
        @DisplayName("adminProperties가 null이면 예외가 발생한다")
        void throws_exception_when_admin_user_ids_null() {
            given(adminProperties.userIds()).willReturn(null);

            assertThatThrownBy(() ->
                    service.sendTestPush(ADMIN_USER_ID, 6L, "테스트", "테스트", null, null)
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("알림 생성")
    class NotificationCreation {

        @Test
        @DisplayName("커스텀 title과 body로 알림을 생성한다")
        void creates_notification_with_custom_title_and_body() {
            Notification savedNotification = Notification.ofCustom(
                    6L, 100L, "커스텀 제목", "커스텀 내용", "https://example.com/thumb.jpg", clock);
            ReflectionTestUtils.setField(savedNotification, "id", 1L);

            given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

            service.sendTestPush(ADMIN_USER_ID, 6L, "커스텀 제목", "커스텀 내용", 100L, "https://example.com/thumb.jpg");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());

            Notification captured = captor.getValue();
            assertThat(captured.getUserId()).isEqualTo(6L);
            assertThat(captured.getTitle()).isEqualTo("커스텀 제목");
            assertThat(captured.getBody()).isEqualTo("커스텀 내용");
            assertThat(captured.getVoteId()).isEqualTo(100L);
            assertThat(captured.getThumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
        }

        @Test
        @DisplayName("voteId와 thumbnailUrl은 null일 수 있다")
        void allows_null_voteId_and_thumbnailUrl() {
            Notification savedNotification = Notification.ofCustom(
                    6L, null, "테스트", "테스트", null, clock);
            ReflectionTestUtils.setField(savedNotification, "id", 1L);

            given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

            Long result = service.sendTestPush(ADMIN_USER_ID, 6L, "테스트", "테스트", null, null);

            assertThat(result).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("이벤트 발행")
    class EventPublishing {

        @Test
        @DisplayName("알림 생성 후 NotificationCreatedEvent를 발행한다")
        void publishes_notification_created_event() {
            Notification savedNotification = Notification.ofCustom(
                    6L, 100L, "테스트", "테스트", null, clock);
            ReflectionTestUtils.setField(savedNotification, "id", 42L);

            given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

            service.sendTestPush(ADMIN_USER_ID, 6L, "테스트", "테스트", 100L, null);

            ArgumentCaptor<NotificationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            NotificationCreatedEvent event = eventCaptor.getValue();
            assertThat(event.notificationIds()).containsExactly(42L);
        }
    }
}
