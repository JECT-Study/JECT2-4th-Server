package com.ject.vs.chat.port.in.dto;

public record SendMessageCommand(Long voteId, Long senderId, String content, Long replyToMessageId) {

    public SendMessageCommand(Long voteId, Long senderId, String content) {
        this(voteId, senderId, content, null);
    }
}
