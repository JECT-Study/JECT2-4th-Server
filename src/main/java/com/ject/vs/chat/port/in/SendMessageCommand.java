package com.ject.vs.chat.port.in;

public record SendMessageCommand(Long voteId, Long senderId, String content) {}
