package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteEndedException;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.in.ImmersiveVoteCommandUseCase.ImmersiveParticipateResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImmersiveVoteCommandServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private ImmersiveVoteCommandService service;

    @Mock private VoteRepository voteRepository;
    @Mock private VoteOptionRepository voteOptionRepository;
    @Mock private VoteParticipationRepository voteParticipationRepository;
    @Mock private GuestFreeVoteService guestFreeVoteService;
    @Mock private Clock clock;

    private Vote ongoingVote() {
        return Vote.create(VoteType.IMMERSIVE, "몰입", null, "t", "img.png",
                Duration.ofHours(24), FIXED_CLOCK);
    }

    private Vote endedVote() {
        return Vote.create(VoteType.IMMERSIVE, "몰입", null, "t", "img.png",
                Duration.ofHours(1), FIXED_CLOCK);
    }

    private void stubOngoing(Long voteId) {
        given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
        given(voteRepository.findById(voteId)).willReturn(Optional.of(ongoingVote()));
        given(voteOptionRepository.existsByIdAndVoteId(10L, voteId)).willReturn(true);
        given(voteOptionRepository.findByVoteIdOrderByPosition(voteId)).willReturn(List.of());
        given(voteParticipationRepository.countByVoteId(voteId)).willReturn(0L);
    }

    @Nested
    class 회원_신규_참여 {

        @Test
        void 신규_참여시_VOTED_반환() {
            stubOngoing(1L);
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.empty());
            given(voteParticipationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ImmersiveParticipateResult result = service.participateOrCancel(1L, 2L, null, 10L);

            assertThat(result.action()).isEqualTo(ImmersiveVoteAction.VOTED);
            assertThat(result.selectedOptionId()).isEqualTo(10L);
            assertThat(result.remainingFreeVotes()).isNull();
            verify(voteParticipationRepository).save(any());
        }

        @Test
        void 다른_옵션_클릭시_VOTED_반환_및_옵션_변경() {
            stubOngoing(1L);
            VoteParticipation existing = VoteParticipation.ofMember(1L, 2L, 10L);
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.of(existing));

            ImmersiveParticipateResult result = service.participateOrCancel(1L, 2L, null, 10L);
            // same option → CANCELED
            assertThat(result.action()).isEqualTo(ImmersiveVoteAction.CANCELED);
        }

        @Test
        void 같은_옵션_재클릭시_CANCELED() {
            stubOngoing(1L);
            VoteParticipation existing = VoteParticipation.ofMember(1L, 2L, 10L);
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.of(existing));

            ImmersiveParticipateResult result = service.participateOrCancel(1L, 2L, null, 10L);

            assertThat(result.action()).isEqualTo(ImmersiveVoteAction.CANCELED);
            assertThat(result.selectedOptionId()).isNull();
            verify(voteParticipationRepository).delete(existing);
        }
    }

    @Nested
    class 비회원_참여 {

        @Test
        void 신규_비회원_참여시_차감_후_VOTED() {
            stubOngoing(1L);
            given(voteParticipationRepository.findByVoteIdAndAnonymousId(1L, "anon")).willReturn(Optional.empty());
            given(voteParticipationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(guestFreeVoteService.remaining("anon")).willReturn(4);

            ImmersiveParticipateResult result = service.participateOrCancel(1L, null, "anon", 10L);

            assertThat(result.action()).isEqualTo(ImmersiveVoteAction.VOTED);
            assertThat(result.remainingFreeVotes()).isEqualTo(4);
            verify(guestFreeVoteService).consume("anon");
        }

        @Test
        void 비회원_옵션_변경시_차감_없음() {
            stubOngoing(1L);
            VoteParticipation existing = VoteParticipation.ofGuest(1L, "anon", 20L);
            given(voteParticipationRepository.findByVoteIdAndAnonymousId(1L, "anon")).willReturn(Optional.of(existing));
            given(guestFreeVoteService.remaining("anon")).willReturn(3);

            service.participateOrCancel(1L, null, "anon", 10L);

            verify(guestFreeVoteService, never()).consume(any());
            assertThat(existing.getOptionId()).isEqualTo(10L);
        }
    }

    @Nested
    class 예외_케이스 {

        @Test
        void 존재하지_않는_투표는_VoteNotFoundException() {
            given(voteRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.participateOrCancel(999L, 1L, null, 10L))
                    .isInstanceOf(VoteNotFoundException.class);
        }

        @Test
        void 종료된_투표는_VoteEndedException() {
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T02:00:00Z"));
            given(voteRepository.findById(1L)).willReturn(Optional.of(endedVote()));

            assertThatThrownBy(() -> service.participateOrCancel(1L, 1L, null, 10L))
                    .isInstanceOf(VoteEndedException.class);
        }
    }
}
