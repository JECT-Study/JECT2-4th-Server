package com.ject.vs.vote.port;

import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.exception.VoteNotEndedException;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.VoteResultDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
class VoteResultQueryServiceTest {

    @Mock VoteRepository voteRepository;
    @Mock VoteOptionRepository voteOptionRepository;
    @Mock VoteParticipationRepository voteParticipationRepository;
    @Mock UserRepository userRepository;

    private VoteResultQueryService service;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);

    private Vote endedVote;
    private Vote ongoingVote;

    @BeforeEach
    void setUp() {
        service = new VoteResultQueryService(
                voteRepository, voteOptionRepository, voteParticipationRepository, userRepository, CLOCK);

        Clock pastClock = Clock.fixed(Instant.parse("2025-05-30T00:00:00Z"), ZoneOffset.UTC);
        endedVote = Vote.create(VoteType.GENERAL, "제목", null, "thumb.png", null,
                Duration.ofHours(24), pastClock);

        Clock recentClock = Clock.fixed(Instant.parse("2025-06-01T00:00:00Z"), ZoneOffset.UTC);
        ongoingVote = Vote.create(VoteType.GENERAL, "진행중", null, "thumb.png", null,
                Duration.ofHours(24), recentClock);
    }

    @Nested
    class getResult {

        @Test
        void 존재하지_않는_투표_예외() {
            given(voteRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getResult(99L, 1L))
                    .isInstanceOf(VoteNotFoundException.class);
        }

        @Test
        void 진행중인_투표_예외() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));

            assertThatThrownBy(() -> service.getResult(1L, 1L))
                    .isInstanceOf(VoteNotEndedException.class);
        }

        @Test
        void 비회원_locked_insight_반환() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(endedVote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(0L);

            VoteResultDetail result = service.getResult(1L, null);

            assertThat(result.insight().locked()).isTrue();
            assertThat(result.insight().scope()).isNull();
            assertThat(result.aiInsight().available()).isFalse();
            assertThat(result.mySelectedOptionId()).isNull();
        }

        @Test
        void 회원_참여O_MY_SELECTION_insight() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(endedVote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(10L);

            VoteParticipation participation = VoteParticipation.ofMember(1L, 1L, 10L);
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(participation));
            given(voteParticipationRepository.countByVoteIdAndOptionId(1L, 10L)).willReturn(6L);
            given(voteParticipationRepository.findGenderDistribution(1L, 10L)).willReturn(List.of());
            given(voteParticipationRepository.findUserIdsByVoteIdAndOptionId(1L, 10L)).willReturn(List.of());
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            VoteResultDetail result = service.getResult(1L, 1L);

            assertThat(result.insight().locked()).isFalse();
            assertThat(result.insight().scope()).isEqualTo(InsightScope.MY_SELECTION);
            assertThat(result.insight().selectionCount()).isEqualTo(6);
            assertThat(result.mySelectedOptionId()).isEqualTo(10L);
        }

        @Test
        void 회원_참여X_TOTAL_insight() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(endedVote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(10L);
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 1L)).willReturn(Optional.empty());
            given(voteParticipationRepository.findGenderDistributionByVote(1L)).willReturn(List.of());
            given(voteParticipationRepository.findAllUserIdsByVoteId(1L)).willReturn(List.of());
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            VoteResultDetail result = service.getResult(1L, 1L);

            assertThat(result.insight().locked()).isFalse();
            assertThat(result.insight().scope()).isEqualTo(InsightScope.TOTAL);
            assertThat(result.insight().selectionCount()).isEqualTo(10);
            assertThat(result.mySelectedOptionId()).isNull();
            assertThat(result.aiInsight().available()).isFalse();
        }

        @Test
        void ai_insight_있으면_available_true() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(endedVote));
            endedVote.cacheAiInsight("헤드라인", "바디");
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(5L);

            VoteParticipation participation = VoteParticipation.ofMember(1L, 1L, 10L);
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(participation));
            given(voteParticipationRepository.countByVoteIdAndOptionId(1L, 10L)).willReturn(3L);
            given(voteParticipationRepository.findGenderDistribution(1L, 10L)).willReturn(List.of());
            given(voteParticipationRepository.findUserIdsByVoteIdAndOptionId(1L, 10L)).willReturn(List.of());
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            VoteResultDetail result = service.getResult(1L, 1L);

            assertThat(result.aiInsight().available()).isTrue();
            assertThat(result.aiInsight().headline()).isEqualTo("헤드라인");
            assertThat(result.aiInsight().body()).isEqualTo("바디");
        }
    }

    @Nested
    class getShareLink {

        @Test
        void 공유링크_반환() {
            given(voteRepository.existsById(1L)).willReturn(true);

            assertThat(service.getShareLink(1L).url())
                    .isEqualTo("https://vs.app/poll/result/1");
        }

        @Test
        void 존재하지_않는_투표_예외() {
            given(voteRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> service.getShareLink(99L))
                    .isInstanceOf(VoteNotFoundException.class);
        }
    }
}
