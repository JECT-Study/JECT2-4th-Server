package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.VoteEmoji;

import java.util.Map;

public interface VoteEmojiCommandUseCase {

    /** emoji == null 이면 취소 */
    EmojiResult reactAsMember(Long voteId, Long userId, VoteEmoji emoji);

    /** emoji == null 이면 취소 */
    EmojiResult reactAsGuest(Long voteId, String anonymousId, VoteEmoji emoji);

    record EmojiResult(Map<VoteEmoji, Long> emojiSummary, long total, VoteEmoji myEmoji) {
    }
}
