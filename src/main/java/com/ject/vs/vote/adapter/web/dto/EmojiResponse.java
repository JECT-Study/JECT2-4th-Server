package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.VoteEmoji;
import com.ject.vs.vote.port.in.VoteEmojiCommandUseCase.EmojiResult;

import java.util.Map;

public record EmojiResponse(EmojiSummary emojiSummary, String myEmoji) {

    public record EmojiSummary(long LIKE, long SAD, long ANGRY, long WOW, long total) {
        public static EmojiSummary from(Map<VoteEmoji, Long> map, long total) {
            return new EmojiSummary(
                    map.getOrDefault(VoteEmoji.LIKE, 0L),
                    map.getOrDefault(VoteEmoji.SAD, 0L),
                    map.getOrDefault(VoteEmoji.ANGRY, 0L),
                    map.getOrDefault(VoteEmoji.WOW, 0L),
                    total
            );
        }
    }

    public static EmojiResponse from(EmojiResult result) {
        String myEmoji = result.myEmoji() != null ? result.myEmoji().name() : null;
        EmojiSummary emojiSummary = EmojiSummary.from(result.emojiSummary(), result.total());
        return new EmojiResponse(emojiSummary, myEmoji);
    }
}
