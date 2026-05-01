package com.ject.vs.chat.domain.event;

import com.ject.vs.chat.domain.ChatMessage;

public record ChatMessageSentEvent(ChatMessage message) {
}
