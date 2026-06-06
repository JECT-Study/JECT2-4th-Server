package com.ject.vs.notification.event;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationRepository;
import com.ject.vs.notification.domain.NotificationType;
import com.ject.vs.notification.port.in.NotificationCommandUseCase;
import com.ject.vs.notification.port.in.NotificationCommandUseCase.NotificationCreateCommand;
import com.ject.vs.notification.port.out.VoteQueryPort;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import com.ject.vs.vote.event.VoteEndedEvent;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoteEndedNotificationHandlerTest {

    @InjectMocks
    private VoteEndedNotificationHandler handler;

    @Mock
    private VoteQueryPort voteQueryPort;

    @Mock
    private VoteParticipationRepository voteParticipationRepository;

    @Mock
    private NotificationCommandUseCase notificationCommandUseCase;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    private static final Long VOTE_ID = 100L;
    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-15T10:00:00Z");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
        lenient().when(clock.getZone()).thenReturn(ZONE_ID);
    }

    private Vote createVote() {
        Vote vote = Vote.create("테스트 투표", null, "thumb.png", null,
                Duration.ofHours(1), clock);
        ReflectionTestUtils.setField(vote, "id", VOTE_ID);
        return vote;
    }

    private Notification createNotificationWithId(Long id, Long userId) {
        Notification notification = Notification.ofVoteEnded(
                userId, VOTE_ID, "테스트 투표", "thumb.png", clock);
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    @Nested
    @DisplayName("on (VoteEndedEvent 핸들러)")
    class OnVoteEnded {

        @Test
        @DisplayName("참여자가 없으면 알림을 생성하지 않는다")
        void does_nothing_when_no_participants() {
            Vote vote = createVote();
            given(voteQueryPort.getById(VOTE_ID)).willReturn(vote);
            given(voteParticipationRepository.findAllUserIdsByVoteId(VOTE_ID)).willReturn(List.of());

            handler.on(new VoteEndedEvent(VOTE_ID));

            verify(notificationCommandUseCase, never()).createBatch(anyList());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("참여자에게 알림을 생성하고 NotificationCreatedEvent를 발행한다")
        void creates_notifications_and_publishes_event() {
            Vote vote = createVote();
            given(voteQueryPort.getById(VOTE_ID)).willReturn(vote);
            given(voteParticipationRepository.findAllUserIdsByVoteId(VOTE_ID))
                    .willReturn(List.of(1L, 2L));
            given(notificationRepository.findUserIdsByVoteIdAndType(VOTE_ID, NotificationType.VOTE_ENDED))
                    .willReturn(List.of());

            Notification n1 = createNotificationWithId(10L, 1L);
            Notification n2 = createNotificationWithId(20L, 2L);
            given(notificationCommandUseCase.createBatch(anyList())).willReturn(List.of(n1, n2));

            handler.on(new VoteEndedEvent(VOTE_ID));

            ArgumentCaptor<NotificationCreatedEvent> eventCaptor =
                    ArgumentCaptor.forClass(NotificationCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().notificationIds()).containsExactly(10L, 20L);
        }

        @Test
        @DisplayName("알림 생성 시 VOTE_ENDED 타입과 투표 정보가 포함된다")
        void creates_commands_with_vote_info() {
            Vote vote = createVote();
            given(voteQueryPort.getById(VOTE_ID)).willReturn(vote);
            given(voteParticipationRepository.findAllUserIdsByVoteId(VOTE_ID))
                    .willReturn(List.of(1L));
            given(notificationRepository.findUserIdsByVoteIdAndType(VOTE_ID, NotificationType.VOTE_ENDED))
                    .willReturn(List.of());

            Notification n1 = createNotificationWithId(10L, 1L);
            given(notificationCommandUseCase.createBatch(anyList())).willReturn(List.of(n1));

            handler.on(new VoteEndedEvent(VOTE_ID));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<NotificationCreateCommand>> commandCaptor =
                    ArgumentCaptor.forClass(List.class);
            verify(notificationCommandUseCase).createBatch(commandCaptor.capture());

            NotificationCreateCommand command = commandCaptor.getValue().getFirst();
            assertThat(command.userId()).isEqualTo(1L);
            assertThat(command.type()).isEqualTo(NotificationType.VOTE_ENDED);
            assertThat(command.voteId()).isEqualTo(VOTE_ID);
            assertThat(command.title()).isEqualTo("테스트 투표");
            assertThat(command.body()).isEqualTo("투표 결과가 공개됐어요");
            assertThat(command.thumbnailUrl()).isEqualTo("thumb.png");
        }

        @Test
        @DisplayName("이미 알림을 받은 참여자는 제외한다")
        void excludes_already_notified_participants() {
            Vote vote = createVote();
            given(voteQueryPort.getById(VOTE_ID)).willReturn(vote);
            given(voteParticipationRepository.findAllUserIdsByVoteId(VOTE_ID))
                    .willReturn(List.of(1L, 2L, 3L));
            given(notificationRepository.findUserIdsByVoteIdAndType(VOTE_ID, NotificationType.VOTE_ENDED))
                    .willReturn(List.of(2L));

            Notification n1 = createNotificationWithId(10L, 1L);
            Notification n3 = createNotificationWithId(30L, 3L);
            given(notificationCommandUseCase.createBatch(anyList())).willReturn(List.of(n1, n3));

            handler.on(new VoteEndedEvent(VOTE_ID));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<NotificationCreateCommand>> commandCaptor =
                    ArgumentCaptor.forClass(List.class);
            verify(notificationCommandUseCase).createBatch(commandCaptor.capture());

            List<Long> targetUserIds = commandCaptor.getValue().stream()
                    .map(NotificationCreateCommand::userId)
                    .toList();
            assertThat(targetUserIds).containsExactly(1L, 3L);
        }

        @Test
        @DisplayName("모든 참여자가 이미 알림을 받았으면 이벤트를 발행하지 않는다")
        void does_not_publish_event_when_all_already_notified() {
            Vote vote = createVote();
            given(voteQueryPort.getById(VOTE_ID)).willReturn(vote);
            given(voteParticipationRepository.findAllUserIdsByVoteId(VOTE_ID))
                    .willReturn(List.of(1L, 2L));
            given(notificationRepository.findUserIdsByVoteIdAndType(VOTE_ID, NotificationType.VOTE_ENDED))
                    .willReturn(List.of(1L, 2L));

            handler.on(new VoteEndedEvent(VOTE_ID));

            verify(notificationCommandUseCase, never()).createBatch(anyList());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("createBatch 결과가 비어 있으면 빈 NotificationCreatedEvent를 발행한다")
        void publishes_empty_event_when_create_batch_returns_empty() {
            Vote vote = createVote();
            given(voteQueryPort.getById(VOTE_ID)).willReturn(vote);
            given(voteParticipationRepository.findAllUserIdsByVoteId(VOTE_ID))
                    .willReturn(List.of(1L));
            given(notificationRepository.findUserIdsByVoteIdAndType(VOTE_ID, NotificationType.VOTE_ENDED))
                    .willReturn(List.of());
            given(notificationCommandUseCase.createBatch(anyList())).willReturn(List.of());

            handler.on(new VoteEndedEvent(VOTE_ID));

            ArgumentCaptor<NotificationCreatedEvent> eventCaptor =
                    ArgumentCaptor.forClass(NotificationCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().notificationIds()).isEmpty();
        }
    }
}