package com.ject.vs.chat.domain.event;

import com.ject.vs.chat.domain.ChatReactionType;

import java.util.Map;

public record ChatReactionUpdatedEvent(
        Long voteId,
        Long messageId,
        Map<ChatReactionType, Long> reactions
) {
}