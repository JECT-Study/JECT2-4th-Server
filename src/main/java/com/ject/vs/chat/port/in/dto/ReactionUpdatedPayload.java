package com.ject.vs.chat.port.in.dto;

import com.ject.vs.chat.domain.ChatReactionType;

import java.util.Map;

public record ReactionUpdatedPayload(
        String event,
        Long messageId,
        Map<ChatReactionType, Long> reactions
) {
    public static ReactionUpdatedPayload of(Long messageId, Map<ChatReactionType, Long> reactions) {
        return new ReactionUpdatedPayload("REACTION_UPDATED", messageId, reactions);
    }
}