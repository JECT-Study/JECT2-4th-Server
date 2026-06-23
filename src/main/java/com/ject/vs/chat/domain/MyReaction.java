package com.ject.vs.chat.domain;

public record MyReaction(
        Long messageId,
        ChatReactionType emoji
) {}
