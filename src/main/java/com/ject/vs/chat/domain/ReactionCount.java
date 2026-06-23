package com.ject.vs.chat.domain;

public record ReactionCount(
        Long messageId,
        ChatReactionType emoji,
        Long count
) {}
