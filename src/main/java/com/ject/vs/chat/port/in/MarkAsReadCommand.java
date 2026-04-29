package com.ject.vs.chat.port.in;

public record MarkAsReadCommand(Long voteId, Long userId, Long lastReadMessageId) {}
