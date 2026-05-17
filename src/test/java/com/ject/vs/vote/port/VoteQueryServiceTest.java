package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.in.VoteQueryUseCase.VoteRatio;
import com.ject.vs.vote.port.in.VoteQueryUseCase.VoteSummary;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ject.vs.vote.domain.Vote;

@ExtendWith(MockitoExtension.class)
class VoteQueryServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private VoteQueryService voteQueryService;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private VoteParticipationRepository voteParticipationRepository;

    @Mock
    private Clock clock;

    @Nested
    class isParticipated {

        @Test
        void 참여한_사용자는_true를_반환한다() {
            given(voteParticipationRepository.existsByVoteIdAndUserId(1L, 2L)).willReturn(true);
            assertThat(voteQueryService.isParticipated(1L, 2L)).isTrue();
        }

        @Test
        void 참여하지_않은_사용자는_false를_반환한다() {
            given(voteParticipationRepository.existsByVoteIdAndUserId(1L, 2L)).willReturn(false);
            assertThat(voteQueryService.isParticipated(1L, 2L)).isFalse();
        }
    }

    @Nested
    class getSelectedOptionId {

        @Test
        void 참여한_경우_optionId를_반환한다() {
            VoteParticipation p = VoteParticipation.ofMember(1L, 2L, 10L);
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.of(p));

            Optional<Long> result = voteQueryService.getSelectedOptionId(1L, 2L);

            assertThat(result).contains(10L);
        }

        @Test
        void 참여하지_않은_경우_empty를_반환한다() {
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.empty());

            Optional<Long> result = voteQueryService.getSelectedOptionId(1L, 2L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class getVoteSummary {

        @Test
        void 진행중인_투표의_summary를_반환한다() {
            Clock nowClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
            Vote vote = Vote.create(VoteType.GENERAL, "제목", null, "thumb", null,
                    Duration.ofHours(24), nowClock);
            given(voteRepository.findById(1L)).willReturn(Optional.of(vote));
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T12:00:00Z"));

            VoteSummary summary = voteQueryService.getVoteSummary(1L);

            assertThat(summary.title()).isEqualTo("제목");
            assertThat(summary.status()).isEqualTo(VoteStatus.ONGOING);
        }

        @Test
        void 존재하지_않는_voteId는_VoteNotFoundException() {
            given(voteRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> voteQueryService.getVoteSummary(999L))
                    .isInstanceOf(VoteNotFoundException.class);
        }
    }

    @Nested
    class getRatio {

        @Test
        void 옵션_비율을_올바르게_계산한다() {
            Vote dummyVote = mock(Vote.class);
            given(dummyVote.getId()).willReturn(1L);
            VoteOption optA = VoteOption.of(dummyVote, "A", 0);
            VoteOption optB = VoteOption.of(dummyVote, "B", 1);
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of(optA, optB));
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(4L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(1L, optA.getId())).willReturn(3L);

            VoteRatio ratio = voteQueryService.getRatio(1L);

            assertThat(ratio.optionARatio()).isEqualTo(75);
            assertThat(ratio.optionBRatio()).isEqualTo(25);
            assertThat(ratio.participantCount()).isEqualTo(4);
        }

        @Test
        void 참여자_없으면_비율_0() {
            Vote dummyVote = mock(Vote.class);
            given(dummyVote.getId()).willReturn(1L);
            VoteOption optA = VoteOption.of(dummyVote, "A", 0);
            VoteOption optB = VoteOption.of(dummyVote, "B", 1);
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of(optA, optB));
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(0L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(1L, optA.getId())).willReturn(0L);

            VoteRatio ratio = voteQueryService.getRatio(1L);

            assertThat(ratio.optionARatio()).isEqualTo(0);
            assertThat(ratio.optionBRatio()).isEqualTo(100);
        }
    }

    @Nested
    class getParticipantCount {

        @Test
        void 참여자_수를_반환한다() {
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(10L);
            assertThat(voteQueryService.getParticipantCount(1L)).isEqualTo(10);
        }
    }

    @Nested
    class findAllVoteIdsByStatus {

        @Test
        void ONGOING_필터_적용() {
            Clock nowClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
            Vote ongoing = Vote.create(VoteType.GENERAL, "진행중", null, "t", null,
                    Duration.ofHours(24), nowClock);
            Vote ended = Vote.create(VoteType.GENERAL, "종료됨", null, "t", null,
                    Duration.ofHours(1), nowClock);

            given(voteRepository.findAllByIdIn(List.of(1L, 2L))).willReturn(List.of(ongoing, ended));
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T02:00:00Z"));

            List<Long> result = voteQueryService.findAllVoteIdsByStatus(List.of(1L, 2L), VoteStatus.ONGOING);

            assertThat(result).hasSize(1);
        }

        @Test
        void 빈_목록을_전달하면_빈_목록을_반환한다() {
            List<Long> result = voteQueryService.findAllVoteIdsByStatus(List.of(), VoteStatus.ONGOING);
            assertThat(result).isEmpty();
        }
    }
}
