package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.port.in.VoteEmojiCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteEmojiCommandService implements VoteEmojiCommandUseCase {

    private final VoteEmojiReactionRepository reactionRepository;

    @Override
    public EmojiResult reactAsMember(Long voteId, Long userId, VoteEmoji emoji) {
        Optional<VoteEmojiReaction> existing = reactionRepository.findByVoteIdAndUserId(voteId, userId);
        VoteEmoji resultEmoji = applyReaction(existing, emoji,
                () -> reactionRepository.save(VoteEmojiReaction.ofMember(voteId, userId, emoji)));
        return buildResult(voteId, resultEmoji);
    }

    @Override
    public EmojiResult reactAsGuest(Long voteId, String anonymousId, VoteEmoji emoji) {
        Optional<VoteEmojiReaction> existing = reactionRepository.findByVoteIdAndAnonymousId(voteId, anonymousId);
        VoteEmoji resultEmoji = applyReaction(existing, emoji,
                () -> reactionRepository.save(VoteEmojiReaction.ofGuest(voteId, anonymousId, emoji)));
        return buildResult(voteId, resultEmoji);
    }

    /**
     * emoji == null → 취소 (delete)
     * 같은 emoji 재클릭 → 취소 (delete)
     * 다른 emoji → 교체 (update)
     * 기존 없음 + emoji 있음 → 신규 (create via newReactionSaver)
     * Returns the current emoji after the operation (null if canceled/deleted).
     */
    private VoteEmoji applyReaction(Optional<VoteEmojiReaction> existing,
                                    VoteEmoji emoji,
                                    Runnable newReactionSaver) {
        if (existing.isPresent()) {
            VoteEmojiReaction reaction = existing.get();
            if (emoji == null || reaction.getEmoji() == emoji) {
                reactionRepository.delete(reaction);
                return null;
            }
            reaction.changeEmoji(emoji);
            return emoji;
        }
        if (emoji == null) return null;
        newReactionSaver.run();
        return emoji;
    }

    private EmojiResult buildResult(Long voteId, VoteEmoji myEmoji) {
        Map<VoteEmoji, Long> summary = Arrays.stream(VoteEmoji.values())
                .collect(Collectors.toMap(e -> e, e -> 0L));

        reactionRepository.countByEmojiForVote(voteId)
                .forEach(row -> summary.put((VoteEmoji) row[0], (Long) row[1]));

        long total = summary.values().stream().mapToLong(Long::longValue).sum();
        return new EmojiResult(summary, total, myEmoji);
    }
}
