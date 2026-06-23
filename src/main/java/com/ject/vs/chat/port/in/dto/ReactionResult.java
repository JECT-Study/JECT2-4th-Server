package com.ject.vs.chat.port.in.dto;

import com.ject.vs.chat.domain.ChatReactionType;

import java.util.Map;

public record ReactionResult(
        Long messageId,
        Map<ChatReactionType, Long> reactions,
        ChatReactionType myReaction
) {}
