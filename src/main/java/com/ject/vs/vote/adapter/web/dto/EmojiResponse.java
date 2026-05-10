package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.VoteEmoji;
import com.ject.vs.vote.port.in.VoteEmojiCommandUseCase.EmojiResult;

import java.util.Map;
import java.util.stream.Collectors;

public record EmojiResponse(Map<VoteEmoji, Long> emojiSummary, long total, String myEmoji) {

    public static EmojiResponse from(EmojiResult result) {
        String myEmoji = result.myEmoji() != null ? result.myEmoji().name() : null;
        return new EmojiResponse(result.emojiSummary(), result.total(), myEmoji);
    }
}
