package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveFeedResult;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveLiveResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ImmersiveVoteQueryServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private ImmersiveVoteQueryService service;

    @Mock private VoteRepository voteRepository;
    @Mock private VoteOptionRepository voteOptionRepository;
    @Mock private VoteParticipationRepository voteParticipationRepository;
    @Mock private Clock clock;

    private Vote makeVote(Duration duration) {
        return Vote.create(VoteType.IMMERSIVE, "몰입", null, "t", "img.png", duration, FIXED_CLOCK);
    }

    @Nested
    class getFeed {

        @Test
        void cursor_없을때_타입_정렬_쿼리_사용() {
            Vote vote = makeVote(Duration.ofHours(24));
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findByTypeOrderByEndAtDesc(eq(VoteType.IMMERSIVE), any()))
                    .willReturn(new SliceImpl<>(List.of(vote), PageRequest.of(0, 10), false));
            given(voteParticipationRepository.countByVoteId(any())).willReturn(5L);

            ImmersiveFeedResult result = service.getFeed(null, 10, null, null);

            assertThat(result.items()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        void cursor_있을때_cursor_기반_쿼리_사용() {
            Vote vote = makeVote(Duration.ofHours(24));
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findByTypeAndIdLessThanOrderByEndAtDesc(
                    eq(VoteType.IMMERSIVE), eq(100L), any()))
                    .willReturn(new SliceImpl<>(List.of(vote), PageRequest.of(0, 10), false));
            given(voteParticipationRepository.countByVoteId(any())).willReturn(3L);

            ImmersiveFeedResult result = service.getFeed(100L, 10, null, null);

            assertThat(result.items()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        void hasNext_true이면_nextCursor_반환() {
            Vote v1 = makeVote(Duration.ofHours(24));
            Vote v2 = makeVote(Duration.ofHours(23));
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findByTypeOrderByEndAtDesc(eq(VoteType.IMMERSIVE), any()))
                    .willReturn(new SliceImpl<>(List.of(v1, v2), PageRequest.of(0, 2), true));
            given(voteParticipationRepository.countByVoteId(any())).willReturn(0L);

            ImmersiveFeedResult result = service.getFeed(null, 2, null, null);

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(result.items().get(1).voteId());
        }

        @Test
        void 회원_userId로_mySelectedOptionId_조회() {
            Vote vote = makeVote(Duration.ofHours(24));
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findByTypeOrderByEndAtDesc(eq(VoteType.IMMERSIVE), any()))
                    .willReturn(new SliceImpl<>(List.of(vote), PageRequest.of(0, 10), false));
            given(voteParticipationRepository.countByVoteId(any())).willReturn(1L);
            VoteParticipation participation = VoteParticipation.ofMember(null, 42L, 99L);
            given(voteParticipationRepository.findByVoteIdAndUserId(any(), eq(42L)))
                    .willReturn(Optional.of(participation));

            ImmersiveFeedResult result = service.getFeed(null, 10, 42L, null);

            assertThat(result.items().get(0).mySelectedOptionId()).isEqualTo(99L);
        }

        @Test
        void 비회원_anonymousId로_mySelectedOptionId_조회() {
            Vote vote = makeVote(Duration.ofHours(24));
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findByTypeOrderByEndAtDesc(eq(VoteType.IMMERSIVE), any()))
                    .willReturn(new SliceImpl<>(List.of(vote), PageRequest.of(0, 10), false));
            given(voteParticipationRepository.countByVoteId(any())).willReturn(1L);
            VoteParticipation participation = VoteParticipation.ofGuest(null, "anon", 77L);
            given(voteParticipationRepository.findByVoteIdAndAnonymousId(any(), eq("anon")))
                    .willReturn(Optional.of(participation));

            ImmersiveFeedResult result = service.getFeed(null, 10, null, "anon");

            assertThat(result.items().get(0).mySelectedOptionId()).isEqualTo(77L);
        }

        @Test
        void 미참여시_mySelectedOptionId_null() {
            Vote vote = makeVote(Duration.ofHours(24));
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));
            given(voteRepository.findByTypeOrderByEndAtDesc(eq(VoteType.IMMERSIVE), any()))
                    .willReturn(new SliceImpl<>(List.of(vote), PageRequest.of(0, 10), false));
            given(voteParticipationRepository.countByVoteId(any())).willReturn(0L);

            ImmersiveFeedResult result = service.getFeed(null, 10, null, null);

            assertThat(result.items().get(0).mySelectedOptionId()).isNull();
        }
    }

    @Nested
    class getLive {

        @Test
        void 참여자_없으면_비율_0() {
            VoteOption optA = VoteOption.of(1L, "A", 1);
            VoteOption optB = VoteOption.of(1L, "B", 2);
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of(optA, optB));
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(0L);

            ImmersiveLiveResult result = service.getLive(1L);

            assertThat(result.optionARatio()).isEqualTo(0);
            assertThat(result.optionBRatio()).isEqualTo(0);
            assertThat(result.participantCount()).isEqualTo(0);
        }

        @Test
        void A_3표_B_1표이면_A비율_75_B비율_25() {
            VoteOption optA = VoteOption.of(1L, "A", 1);
            VoteOption optB = VoteOption.of(1L, "B", 2);
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of(optA, optB));
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(4L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(eq(1L), any())).willReturn(3L);

            ImmersiveLiveResult result = service.getLive(1L);

            assertThat(result.optionARatio()).isEqualTo(75);
            assertThat(result.optionBRatio()).isEqualTo(25);
            assertThat(result.participantCount()).isEqualTo(4);
        }

        @Test
        void currentViewerCount_항상_0() {
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(0L);

            ImmersiveLiveResult result = service.getLive(1L);

            assertThat(result.currentViewerCount()).isEqualTo(0);
        }
    }
}
