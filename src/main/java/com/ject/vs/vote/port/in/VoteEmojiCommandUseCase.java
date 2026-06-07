package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.EmojiAction;
import com.ject.vs.vote.domain.VoteEmoji;

import java.util.Map;

public interface VoteEmojiCommandUseCase {

    /** emoji == null 이면 취소 */
    EmojiResult reactAsMember(Long voteId, Long userId, VoteEmoji emoji);

    /** emoji == null 이면 취소 */
    EmojiResult reactAsGuest(Long voteId, String anonymousId, VoteEmoji emoji);

    /**
     * @param action 이전 반응 상태와 비교한 동작 분류(행동 로그 emoji_reacted의 action 변수)
     */
    record EmojiResult(Map<VoteEmoji, Long> emojiSummary, long total, VoteEmoji myEmoji, EmojiAction action) {
    }
}
