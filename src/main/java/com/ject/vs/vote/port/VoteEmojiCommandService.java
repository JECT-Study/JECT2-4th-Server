package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteEndedException;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.in.VoteEmojiCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteEmojiCommandService implements VoteEmojiCommandUseCase {

    private final VoteEmojiReactionRepository reactionRepository;
    private final VoteRepository voteRepository;
    private final Clock clock;

    @Override
    public EmojiResult reactAsMember(Long voteId, Long userId, VoteEmoji emoji) {
        Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
        if (vote.isEnded(clock)) throw new VoteEndedException();
        Optional<VoteEmojiReaction> existing = reactionRepository.findByVoteIdAndUserId(voteId, userId);
        EmojiAction action = resolveAction(existing, emoji);
        VoteEmojiReaction resultEmoji = applyReaction(existing, emoji != null ? VoteEmojiReaction.ofMember(voteId, userId, emoji) : null);
        return buildResult(voteId, resultEmoji != null ? resultEmoji.getEmoji() : null, action);
    }

    @Override
    public EmojiResult reactAsGuest(Long voteId, String anonymousId, VoteEmoji emoji) {
        Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
        if (vote.isEnded(clock)) throw new VoteEndedException();
        Optional<VoteEmojiReaction> existing = reactionRepository.findByVoteIdAndAnonymousId(voteId, anonymousId);
        EmojiAction action = resolveAction(existing, emoji);
        VoteEmojiReaction resultEmoji = applyReaction(
                existing,
                emoji != null ? VoteEmojiReaction.ofGuest(voteId, anonymousId, emoji) : null
        );
        return buildResult(voteId, resultEmoji != null ? resultEmoji.getEmoji() : null, action);
    }

    /**
     * 행동 로그(emoji_reacted)의 action 변수를 위해, 저장 전 이전 반응 상태와 비교하여 동작을 분류한다.
     * - 기존 없음 + emoji 있음 → CREATED
     * - 기존 있음 + (emoji 없음 or 같은 emoji 재클릭) → CANCELED
     * - 기존 있음 + 다른 emoji → CHANGED
     * - 기존 없음 + emoji 없음 → CANCELED (멱등 no-op)
     */
    private EmojiAction resolveAction(Optional<VoteEmojiReaction> existing, VoteEmoji emoji) {
        if (existing.isEmpty()) {
            return emoji != null ? EmojiAction.CREATED : EmojiAction.CANCELED;
        }
        if (emoji == null || existing.get().getEmoji() == emoji) {
            return EmojiAction.CANCELED;
        }
        return EmojiAction.CHANGED;
    }

    /**
     * emoji == null → 취소 (delete)
     * 같은 emoji 재클릭 → 취소 (delete)
     * 다른 emoji → 교체 (update)
     * 기존 없음 + emoji 있음 → 신규 (create via newReactionSaver)
     * Returns the current emoji after the operation (null if canceled/deleted).
     */
    private VoteEmojiReaction applyReaction(Optional<VoteEmojiReaction> existing,
                                            VoteEmojiReaction emojiReaction) {
        if (existing.isEmpty()) {
            if (emojiReaction != null) {
                reactionRepository.save(emojiReaction);
                return emojiReaction;
            }
            return null;
        }

        VoteEmojiReaction existingReaction = existing.get();

        if (emojiReaction == null || existingReaction.getEmoji() == emojiReaction.getEmoji()) {
            reactionRepository.delete(existingReaction);
            return null;
        }

        existingReaction.changeEmoji(emojiReaction.getEmoji());
        reactionRepository.save(existingReaction);
        return existingReaction;
    }

    private EmojiResult buildResult(Long voteId, VoteEmoji myEmoji, EmojiAction action) {
        Map<VoteEmoji, Long> summary = VoteEmoji.getMap();

        reactionRepository.countByEmojiForVote(voteId)
                .forEach(row -> summary.put(row.emoij(), row.count()));

        long total = summary.values().stream().mapToLong(Long::longValue).sum();
        return new EmojiResult(summary, total, myEmoji, action);
    }
}
