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
        VoteEmojiReaction resultEmoji = applyReaction(existing, emoji != null ? VoteEmojiReaction.ofMember(voteId, userId, emoji) : null);
        return buildResult(voteId, resultEmoji != null ? resultEmoji.getEmoji() : null);
    }

    @Override
    public EmojiResult reactAsGuest(Long voteId, String anonymousId, VoteEmoji emoji) {
        Optional<VoteEmojiReaction> existing = reactionRepository.findByVoteIdAndAnonymousId(voteId, anonymousId);
        VoteEmojiReaction resultEmoji = applyReaction(
                existing,
                emoji != null ? VoteEmojiReaction.ofGuest(voteId, anonymousId, emoji) : null
        );
        return buildResult(voteId, resultEmoji != null ? resultEmoji.getEmoji() : null);
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
        existing.ifPresent(reactionRepository::delete);
        if (emojiReaction == null) return null;
        reactionRepository.save(emojiReaction);
        return emojiReaction;
    }

    private EmojiResult buildResult(Long voteId, VoteEmoji myEmoji) {
        Map<VoteEmoji, Long> summary = VoteEmoji.getMap();

        reactionRepository.countByEmojiForVote(voteId)
                .forEach(row -> summary.put(row.emoij(), row.count()));

        long total = summary.values().stream().mapToLong(Long::longValue).sum();
        return new EmojiResult(summary, total, myEmoji);
    }
}
