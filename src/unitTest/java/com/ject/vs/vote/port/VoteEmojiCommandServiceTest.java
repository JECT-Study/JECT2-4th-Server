package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.VoteEmoji;
import com.ject.vs.vote.domain.VoteEmojiReaction;
import com.ject.vs.vote.domain.VoteEmojiReactionRepository;
import com.ject.vs.vote.port.in.VoteEmojiCommandUseCase.EmojiResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoteEmojiCommandServiceTest {

    @InjectMocks
    private VoteEmojiCommandService service;

    @Mock
    private VoteEmojiReactionRepository reactionRepository;

    private void stubEmptySummary(Long voteId) {
        given(reactionRepository.countByEmojiForVote(voteId)).willReturn(List.of());
    }

    @Nested
    class reactAsMember {

        @Test
        void 기존_반응_없으면_새로_저장한다() {
            given(reactionRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.empty());
            given(reactionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            stubEmptySummary(1L);

            EmojiResult result = service.reactAsMember(1L, 2L, VoteEmoji.LIKE);

            verify(reactionRepository).save(any());
            assertThat(result.myEmoji()).isEqualTo(VoteEmoji.LIKE);
        }

        @Test
        void 같은_이모지_재클릭시_취소한다() {
            VoteEmojiReaction existing = VoteEmojiReaction.ofMember(1L, 2L, VoteEmoji.LIKE);
            given(reactionRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.of(existing));
            stubEmptySummary(1L);

            EmojiResult result = service.reactAsMember(1L, 2L, VoteEmoji.LIKE);

            verify(reactionRepository).delete(existing);
            assertThat(result.myEmoji()).isNull();
        }

        @Test
        void 다른_이모지로_교체한다() {
            VoteEmojiReaction existing = VoteEmojiReaction.ofMember(1L, 2L, VoteEmoji.LIKE);
            given(reactionRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.of(existing));
            stubEmptySummary(1L);

            EmojiResult result = service.reactAsMember(1L, 2L, VoteEmoji.WOW);

            assertThat(existing.getEmoji()).isEqualTo(VoteEmoji.WOW);
            assertThat(result.myEmoji()).isEqualTo(VoteEmoji.WOW);
        }

        @Test
        void null_전송시_기존_반응을_취소한다() {
            VoteEmojiReaction existing = VoteEmojiReaction.ofMember(1L, 2L, VoteEmoji.SAD);
            given(reactionRepository.findByVoteIdAndUserId(1L, 2L)).willReturn(Optional.of(existing));
            stubEmptySummary(1L);

            EmojiResult result = service.reactAsMember(1L, 2L, null);

            verify(reactionRepository).delete(existing);
            assertThat(result.myEmoji()).isNull();
        }
    }

    @Nested
    class reactAsGuest {

        @Test
        void 비회원_신규_반응을_저장한다() {
            given(reactionRepository.findByVoteIdAndAnonymousId(1L, "anon")).willReturn(Optional.empty());
            given(reactionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            stubEmptySummary(1L);

            EmojiResult result = service.reactAsGuest(1L, "anon", VoteEmoji.ANGRY);

            verify(reactionRepository).save(any());
            assertThat(result.myEmoji()).isEqualTo(VoteEmoji.ANGRY);
        }
    }
}
