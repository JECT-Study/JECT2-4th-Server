package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.VoteEmoji;
import com.ject.vs.vote.port.in.VoteEmojiCommandUseCase.EmojiResult;

import java.util.Map;
import java.util.stream.Collectors;

public record EmojiResponse(Map<String, Long> emojiSummary, long total, String myEmoji) {

    public static EmojiResponse from(EmojiResult result) {
        Map<String, Long> summary = result.emojiSummary().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
        String myEmoji = result.myEmoji() != null ? result.myEmoji().name() : null;
        return new EmojiResponse(summary, result.total(), myEmoji);
    }
}
