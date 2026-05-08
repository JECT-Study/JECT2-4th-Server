package com.ject.vs.vote.scheduler;

import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteRepository;
import com.ject.vs.vote.domain.VoteStatus;
import com.ject.vs.vote.domain.VoteType;
import com.ject.vs.vote.event.VoteEndedEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoteCloseSchedulerTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private VoteCloseScheduler scheduler;

    @Mock private VoteRepository voteRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Clock clock;

    private Vote makeExpiredVote() {
        // duration 1h, clock은 생성 시점이 FIXED_CLOCK → endAt = T+1h
        // 스케줄러가 T+2h에 실행되면 이미 만료
        return Vote.create(VoteType.GENERAL, "test", null, "thumb.png", null,
                Duration.ofHours(1), FIXED_CLOCK);
    }

    @Nested
    class closeExpiredVotes {

        @Test
        void 만료된_투표가_있으면_markEnded_호출하고_이벤트_발행() {
            Vote expired = makeExpiredVote();
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T02:00:00Z"));
            given(voteRepository.findExpiredOngoing(any())).willReturn(List.of(expired));

            scheduler.closeExpiredVotes();

            assertThat(expired.getStatus()).isEqualTo(VoteStatus.ENDED);

            ArgumentCaptor<VoteEndedEvent> captor = ArgumentCaptor.forClass(VoteEndedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(VoteEndedEvent.class);
        }

        @Test
        void 만료된_투표_없으면_이벤트_미발행() {
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T02:00:00Z"));
            given(voteRepository.findExpiredOngoing(any())).willReturn(List.of());

            scheduler.closeExpiredVotes();

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void 만료된_투표_여러개이면_각각_이벤트_발행() {
            Vote v1 = makeExpiredVote();
            Vote v2 = makeExpiredVote();
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T02:00:00Z"));
            given(voteRepository.findExpiredOngoing(any())).willReturn(List.of(v1, v2));

            scheduler.closeExpiredVotes();

            assertThat(v1.getStatus()).isEqualTo(VoteStatus.ENDED);
            assertThat(v2.getStatus()).isEqualTo(VoteStatus.ENDED);
            verify(eventPublisher, times(2)).publishEvent(any(VoteEndedEvent.class));
        }

        @Test
        void findExpiredOngoing에_현재_시각_전달() {
            Instant now = Instant.parse("2025-06-01T12:00:00Z");
            given(clock.instant()).willReturn(now);
            given(voteRepository.findExpiredOngoing(now)).willReturn(List.of());

            scheduler.closeExpiredVotes();

            verify(voteRepository).findExpiredOngoing(now);
        }
    }
}
