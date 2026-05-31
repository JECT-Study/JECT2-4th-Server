package com.ject.vs.notification.port;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationRepository;
import com.ject.vs.notification.port.in.NotificationQueryUseCase.NotificationPageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @InjectMocks
    private NotificationQueryService service;

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
    @DisplayName("getList")
    class GetList {

        @Test
        @DisplayName("알림 목록을 조회한다")
        void returns_notification_list() {
            Notification notification = Notification.ofVoteEnded(1L, 100L, "테스트 투표", "https://example.com/thumb.jpg", clock);
            SliceImpl<Notification> slice = new SliceImpl<>(List.of(notification), PageRequest.ofSize(20), false);

            given(notificationRepository.findPage(eq(1L), isNull(), any(Instant.class), any(PageRequest.class)))
                    .willReturn(slice);

            NotificationPageResult result = service.getList(1L, null, 20);

            assertThat(result.notifications()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("커서 기반 페이지네이션이 동작한다")
        void pagination_with_cursor() {
            Notification notification = Notification.ofVoteEnded(1L, 100L, "테스트 투표", null, clock);
            SliceImpl<Notification> slice = new SliceImpl<>(List.of(notification), PageRequest.ofSize(20), true);

            given(notificationRepository.findPage(eq(1L), eq(50L), any(Instant.class), any(PageRequest.class)))
                    .willReturn(slice);

            NotificationPageResult result = service.getList(1L, 50L, 20);

            assertThat(result.notifications()).hasSize(1);
            assertThat(result.hasNext()).isTrue();
            verify(notificationRepository).findPage(eq(1L), eq(50L), any(Instant.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("빈 목록을 반환할 수 있다")
        void returns_empty_list() {
            SliceImpl<Notification> slice = new SliceImpl<>(List.of(), PageRequest.ofSize(20), false);

            given(notificationRepository.findPage(eq(1L), isNull(), any(Instant.class), any(PageRequest.class)))
                    .willReturn(slice);

            NotificationPageResult result = service.getList(1L, null, 20);

            assertThat(result.notifications()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("1개월 이내의 알림만 조회한다")
        void filters_by_one_month() {
            SliceImpl<Notification> slice = new SliceImpl<>(List.of(), PageRequest.ofSize(20), false);

            given(notificationRepository.findPage(eq(1L), isNull(), any(Instant.class), any(PageRequest.class)))
                    .willReturn(slice);

            service.getList(1L, null, 20);

            // 1개월 전 시점이 파라미터로 전달되었는지 확인
            verify(notificationRepository).findPage(
                    eq(1L),
                    isNull(),
                    eq(FIXED_INSTANT.minus(java.time.Duration.ofDays(30))),
                    any(PageRequest.class)
            );
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("읽지 않은 알림 개수를 반환한다")
        void returns_unread_count() {
            given(notificationRepository.countByUserIdAndIsReadFalse(1L)).willReturn(5L);

            long result = service.getUnreadCount(1L);

            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("읽지 않은 알림이 없으면 0을 반환한다")
        void returns_zero_when_no_unread() {
            given(notificationRepository.countByUserIdAndIsReadFalse(1L)).willReturn(0L);

            long result = service.getUnreadCount(1L);

            assertThat(result).isEqualTo(0L);
        }
    }
}
