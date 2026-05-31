package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.InvalidOptionException;
import com.ject.vs.vote.exception.VoteEndedException;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.in.VoteCommandUseCase;
import com.ject.vs.vote.port.in.VoteCommandUseCase.ParticipateResult;
import com.ject.vs.vote.port.in.VoteCommandUseCase.VoteCreateCommand;
import com.ject.vs.vote.port.in.VoteCommandUseCase.VoteCreateResult;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoteCommandServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private VoteCommandService service;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private VoteParticipationRepository voteParticipationRepository;

    @Mock
    private GuestFreeVoteService guestFreeVoteService;

    @Mock
    private Clock clock;

    @Nested
    class create {

        @Test
        void 정상적으로_투표를_생성한다() {
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            VoteCreateCommand cmd = new VoteCreateCommand(
                    "제목", null, "thumb", null,
                    VoteDuration.HOURS_24, "A", "B");

            Vote fakeVote = Vote.create("제목", null, "thumb", null,
                    Duration.ofHours(24), FIXED_CLOCK);
            given(voteRepository.save(any())).willReturn(fakeVote);
            VoteOption optA = VoteOption.of(fakeVote, "A", 0);
            VoteOption optB = VoteOption.of(fakeVote, "B", 1);
            given(voteOptionRepository.save(any())).willReturn(optA, optB);

            VoteCreateResult result = service.create(cmd);

            assertThat(result.status()).isEqualTo(VoteStatus.ONGOING);
        }

    }

    @Nested
    class participateAsMember {

        @Test
        void 신규_회원_참여_정상_저장() {
            Vote ongoingVote = Vote.create("투표", null, "t", null,
                    Duration.ofHours(24), FIXED_CLOCK);
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));
            given(voteOptionRepository.existsByIdAndVoteId(10L, 1L)).willReturn(true);
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.empty());
            given(voteParticipationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(1L);

            ParticipateResult result = service.participateAsMember(1L, 2L, 10L);

            assertThat(result.selectedOptionId()).isEqualTo(10L);
            assertThat(result.participantCount()).isEqualTo(1);
            verify(voteParticipationRepository).save(any());
        }

        @Test
        void 기존_참여자는_옵션을_변경한다() {
            Vote ongoingVote = Vote.create("투표", null, "t", null,
                    Duration.ofHours(24), FIXED_CLOCK);
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));
            given(voteOptionRepository.existsByIdAndVoteId(20L, 1L)).willReturn(true);
            VoteParticipation existing = VoteParticipation.ofMember(1L, 2L, 10L);
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.of(existing));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(1L);

            service.participateAsMember(1L, 2L, 20L);

            assertThat(existing.getOptionId()).isEqualTo(20L);
        }

        @Test
        void 종료된_투표에_참여하면_VoteEndedException() {
            Vote endedVote = Vote.create("투표", null, "t", null,
                    Duration.ofHours(1), FIXED_CLOCK);
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T02:00:00Z"));
            given(voteRepository.findById(1L)).willReturn(Optional.of(endedVote));

            assertThatThrownBy(() -> service.participateAsMember(1L, 2L, 10L))
                    .isInstanceOf(VoteEndedException.class);
        }

        @Test
        void 존재하지_않는_투표는_VoteNotFoundException() {
            given(voteRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.participateAsMember(999L, 2L, 10L))
                    .isInstanceOf(VoteNotFoundException.class);
        }

        @Test
        void 유효하지_않은_optionId는_InvalidOptionException() {
            Vote ongoingVote = Vote.create("투표", null, "t", null,
                    Duration.ofHours(24), FIXED_CLOCK);
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));
            given(voteOptionRepository.existsByIdAndVoteId(99L, 1L)).willReturn(false);

            assertThatThrownBy(() -> service.participateAsMember(1L, 2L, 99L))
                    .isInstanceOf(InvalidOptionException.class);
        }
    }

    @Nested
    class participateAsGuest {

        @Test
        void 신규_비회원은_차감_후_참여_저장() {
            Vote ongoingVote = Vote.create("투표", null, "t", null,
                    Duration.ofHours(24), FIXED_CLOCK);
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));
            given(voteOptionRepository.existsByIdAndVoteId(10L, 1L)).willReturn(true);
            given(voteParticipationRepository.findByVoteIdAndAnonymousId(1L, "anon")).willReturn(Optional.empty());
            given(voteParticipationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(guestFreeVoteService.remaining("anon")).willReturn(4);
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(1L);

            ParticipateResult result = service.participateAsGuest(1L, "anon", 10L);

            verify(guestFreeVoteService).consume("anon");
            assertThat(result.remainingFreeVotes()).isEqualTo(4);
        }

        @Test
        void 기존_비회원은_옵션만_변경하고_차감_없음() {
            Vote ongoingVote = Vote.create("투표", null, "t", null,
                    Duration.ofHours(24), FIXED_CLOCK);
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));
            given(voteOptionRepository.existsByIdAndVoteId(20L, 1L)).willReturn(true);
            VoteParticipation existing = VoteParticipation.ofGuest(1L, "anon", 10L);
            given(voteParticipationRepository.findByVoteIdAndAnonymousId(1L, "anon")).willReturn(Optional.of(existing));
            given(guestFreeVoteService.remaining("anon")).willReturn(3);
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(1L);

            service.participateAsGuest(1L, "anon", 20L);

            verify(guestFreeVoteService, org.mockito.Mockito.never()).consume(any());
            assertThat(existing.getOptionId()).isEqualTo(20L);
        }
    }

    @Nested
    class cancel {

        @Test
        void 정상적으로_참여를_취소한다() {
            Vote ongoingVote = Vote.create("투표", null, "t", null,
                    Duration.ofHours(24), FIXED_CLOCK);
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));

            service.cancel(1L, 2L);

            verify(voteParticipationRepository).deleteByVoteIdAndUserId(1L, 2L);
        }

        @Test
        void 종료된_투표_취소는_VoteEndedException() {
            Vote endedVote = Vote.create("투표", null, "t", null,
                    Duration.ofHours(1), FIXED_CLOCK);
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T02:00:00Z"));
            given(voteRepository.findById(1L)).willReturn(Optional.of(endedVote));

            assertThatThrownBy(() -> service.cancel(1L, 2L))
                    .isInstanceOf(VoteEndedException.class);
        }
    }
}
