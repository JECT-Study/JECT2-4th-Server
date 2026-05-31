package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.VoteDetailQueryService.VoteDetailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class VoteDetailQueryServiceTest {

    @Mock VoteRepository voteRepository;
    @Mock VoteOptionRepository voteOptionRepository;
    @Mock VoteParticipationRepository voteParticipationRepository;
    @Mock VoteEmojiReactionRepository emojiReactionRepository;

    private VoteDetailQueryService service;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);

    private Vote ongoingVote;
    private Vote endedVote;

    @BeforeEach
    void setUp() {
        service = new VoteDetailQueryService(
                voteRepository, voteOptionRepository, voteParticipationRepository, emojiReactionRepository, CLOCK);

        // 진행중 투표 (현재 시간 기준 24시간 후 종료)
        Clock recentClock = Clock.fixed(Instant.parse("2025-06-01T00:00:00Z"), ZoneOffset.UTC);
        ongoingVote = Vote.create("진행중 투표", null, "thumb.png", null,
                Duration.ofHours(24), recentClock);

        // 종료된 투표 (현재 시간 기준 이미 종료)
        Clock pastClock = Clock.fixed(Instant.parse("2025-05-30T00:00:00Z"), ZoneOffset.UTC);
        endedVote = Vote.create("종료된 투표", null, "thumb.png", null,
                Duration.ofHours(24), pastClock);
    }

    @Nested
    @DisplayName("getDetail 메서드")
    class getDetail {

        @Test
        @DisplayName("존재하지 않는 투표 조회 시 예외 발생")
        void 존재하지_않는_투표_예외() {
            given(voteRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDetail(99L, null, null))
                    .isInstanceOf(VoteNotFoundException.class);
        }

        @Test
        @DisplayName("진행중 투표 - 상태가 ONGOING")
        void 진행중_투표_상태_ONGOING() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(0L);
            given(emojiReactionRepository.countByEmojiForVote(1L)).willReturn(List.of());

            VoteDetailResult result = service.getDetail(1L, null, null);

            assertThat(result.status()).isEqualTo(VoteStatus.ONGOING);
        }

        @Test
        @DisplayName("종료된 투표 - 상태가 ENDED")
        void 종료된_투표_상태_ENDED() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(endedVote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(0L);
            given(emojiReactionRepository.countByEmojiForVote(1L)).willReturn(List.of());

            VoteDetailResult result = service.getDetail(1L, null, null);

            assertThat(result.status()).isEqualTo(VoteStatus.ENDED);
        }

        @Test
        @DisplayName("회원 투표 참여 정보 조회")
        void 회원_투표_참여_정보_조회() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(10L);
            given(emojiReactionRepository.countByEmojiForVote(1L)).willReturn(List.of());

            VoteParticipation participation = VoteParticipation.ofMember(1L, 42L, 10L);
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 42L))
                    .willReturn(Optional.of(participation));
            given(emojiReactionRepository.findByVoteIdAndUserId(1L, 42L)).willReturn(Optional.empty());

            VoteDetailResult result = service.getDetail(1L, 42L, null);

            assertThat(result.voted()).isTrue();
            assertThat(result.mySelectedOptionId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("비회원 투표 참여 정보 조회 (anonymousId)")
        void 비회원_투표_참여_정보_조회() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(5L);
            given(emojiReactionRepository.countByEmojiForVote(1L)).willReturn(List.of());

            VoteParticipation participation = VoteParticipation.ofGuest(1L, "anon-123", 20L);
            given(voteParticipationRepository.findByVoteIdAndAnonymousId(1L, "anon-123"))
                    .willReturn(Optional.of(participation));
            given(emojiReactionRepository.findByVoteIdAndAnonymousId(1L, "anon-123")).willReturn(Optional.empty());

            VoteDetailResult result = service.getDetail(1L, null, "anon-123");

            assertThat(result.voted()).isTrue();
            assertThat(result.mySelectedOptionId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("미투표시 voted=false, mySelectedOptionId=null")
        void 미투표시_정보() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(0L);
            given(emojiReactionRepository.countByEmojiForVote(1L)).willReturn(List.of());

            VoteDetailResult result = service.getDetail(1L, null, null);

            assertThat(result.voted()).isFalse();
            assertThat(result.mySelectedOptionId()).isNull();
        }

        @Test
        @DisplayName("옵션별 득표수와 비율 계산")
        void 옵션별_득표수_비율_계산() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));

            Vote dummyVote = ongoingVote;
            VoteOption optA = VoteOption.of(dummyVote, "짜장면", 1);
            VoteOption optB = VoteOption.of(dummyVote, "짬뽕", 2);
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of(optA, optB));

            given(voteParticipationRepository.countByVoteId(1L)).willReturn(100L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(eq(1L), any()))
                    .willReturn(60L, 40L);
            given(emojiReactionRepository.countByEmojiForVote(1L)).willReturn(List.of());

            VoteDetailResult result = service.getDetail(1L, null, null);

            assertThat(result.participantCount()).isEqualTo(100);
            assertThat(result.options()).hasSize(2);
            assertThat(result.options().get(0).label()).isEqualTo("짜장면");
            assertThat(result.options().get(0).voteCount()).isEqualTo(60L);
            assertThat(result.options().get(0).ratio()).isEqualTo(60);
            assertThat(result.options().get(1).label()).isEqualTo("짬뽕");
            assertThat(result.options().get(1).voteCount()).isEqualTo(40L);
            assertThat(result.options().get(1).ratio()).isEqualTo(40);
        }

        @Test
        @DisplayName("참여자 없으면 비율 0")
        void 참여자_없으면_비율_0() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));

            VoteOption optA = VoteOption.of(ongoingVote, "옵션A", 1);
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of(optA));

            given(voteParticipationRepository.countByVoteId(1L)).willReturn(0L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(eq(1L), any())).willReturn(0L);
            given(emojiReactionRepository.countByEmojiForVote(1L)).willReturn(List.of());

            VoteDetailResult result = service.getDetail(1L, null, null);

            assertThat(result.participantCount()).isEqualTo(0);
            assertThat(result.options().get(0).ratio()).isEqualTo(0);
        }

        @Test
        @DisplayName("이모지 반응 조회 - 회원")
        void 이모지_반응_조회_회원() {
            given(voteRepository.findById(1L)).willReturn(Optional.of(ongoingVote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(1L)).willReturn(List.of());
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(0L);

            List<EmoijCount> emojiCounts = List.of(
                    new EmoijCount(VoteEmoji.LIKE, 50L),
                    new EmoijCount(VoteEmoji.WOW, 30L)
            );
            given(emojiReactionRepository.countByEmojiForVote(1L)).willReturn(emojiCounts);

            VoteEmojiReaction myReaction = VoteEmojiReaction.ofMember(1L, 42L, VoteEmoji.LIKE);
            given(emojiReactionRepository.findByVoteIdAndUserId(1L, 42L))
                    .willReturn(Optional.of(myReaction));

            VoteDetailResult result = service.getDetail(1L, 42L, null);

            assertThat(result.emojiSummary().get(VoteEmoji.LIKE)).isEqualTo(50L);
            assertThat(result.emojiSummary().get(VoteEmoji.WOW)).isEqualTo(30L);
            assertThat(result.emojiSummary().get(VoteEmoji.SAD)).isEqualTo(0L);
            assertThat(result.emojiSummary().get(VoteEmoji.ANGRY)).isEqualTo(0L);
            assertThat(result.myEmoji()).isEqualTo(VoteEmoji.LIKE);
        }
    }
}
