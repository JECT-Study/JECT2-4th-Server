package com.ject.vs.notification.port;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationRepository;
import com.ject.vs.notification.domain.NotificationType;
import com.ject.vs.notification.exception.NotificationNotFoundException;
import com.ject.vs.notification.port.in.NotificationCommandUseCase.NotificationCreateCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @InjectMocks
    private NotificationCommandService service;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private Clock clock;

    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-15T10:00:00Z");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
        lenient().when(clock.getZone()).thenReturn(ZONE_ID);
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("존재하지 않는 알림은 NotificationNotFoundException이 발생한다")
        void throws_exception_when_notification_not_found() {
            given(notificationRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.markAsRead(999L, 1L))
                    .isInstanceOf(NotificationNotFoundException.class);
        }

        @Test
        @DisplayName("다른 사용자의 알림은 NotificationNotFoundException이 발생한다")
        void throws_exception_when_not_owner() {
            Notification notification = Notification.ofVoteEnded(2L, 100L, "테스트 투표", null, clock);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            assertThatThrownBy(() -> service.markAsRead(1L, 1L))
                    .isInstanceOf(NotificationNotFoundException.class);
        }

        @Test
        @DisplayName("본인의 알림은 읽음 처리된다")
        void marks_own_notification_as_read() {
            Notification notification = Notification.ofVoteEnded(1L, 100L, "테스트 투표", null, clock);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            service.markAsRead(1L, 1L);

            assertThat(notification.isRead()).isTrue();
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("사용자의 모든 미읽음 알림을 읽음 처리하고 개수를 반환한다")
        void marks_all_as_read_and_returns_count() {
            given(notificationRepository.markAllAsRead(eq(1L), any(Instant.class))).willReturn(5);

            int result = service.markAllAsRead(1L);

            assertThat(result).isEqualTo(5);
            verify(notificationRepository).markAllAsRead(eq(1L), any(Instant.class));
        }

        @Test
        @DisplayName("미읽음 알림이 없으면 0을 반환한다")
        void returns_zero_when_no_unread() {
            given(notificationRepository.markAllAsRead(eq(1L), any(Instant.class))).willReturn(0);

            int result = service.markAllAsRead(1L);

            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("createBatch")
    class CreateBatch {

        @Test
        @DisplayName("빈 리스트가 들어오면 빈 리스트를 반환한다")
        void returns_empty_list_when_commands_empty() {
            List<Notification> result = service.createBatch(List.of());

            assertThat(result).isEmpty();
            verify(notificationRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("VOTE_ENDED 타입의 알림을 생성한다")
        void creates_vote_ended_notifications() {
            List<NotificationCreateCommand> commands = List.of(
                    new NotificationCreateCommand(1L, NotificationType.VOTE_ENDED, 100L,
                            "투표 종료", "테스트 투표가 종료되었습니다", "https://example.com/thumb.jpg"),
                    new NotificationCreateCommand(2L, NotificationType.VOTE_ENDED, 100L,
                            "투표 종료", "테스트 투표가 종료되었습니다", null)
            );

            given(notificationRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

            List<Notification> result = service.createBatch(commands);

            assertThat(result).hasSize(2);
            verify(notificationRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("지원하지 않는 타입은 IllegalArgumentException이 발생한다")
        void throws_exception_for_unsupported_type() {
            // 현재 VOTE_ENDED만 지원하므로, 다른 타입이 추가되면 이 테스트가 필요
            // NotificationType enum에 새 타입 추가 시 테스트
        }
    }
}
