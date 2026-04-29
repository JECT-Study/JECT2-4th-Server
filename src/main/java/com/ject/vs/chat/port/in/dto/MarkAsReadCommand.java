package com.ject.vs.chat.port.in.dto;

public record MarkAsReadCommand(Long voteId, Long userId, Long lastReadMessageId) {}
