package com.ject.vs.chat.port.in.dto;

public record SendMessageCommand(Long voteId, Long senderId, String content) {}
