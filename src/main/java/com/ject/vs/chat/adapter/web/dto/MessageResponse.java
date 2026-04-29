package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.MessageResult;

import java.time.Instant;

public record MessageResponse(
        Long messageId,
        String content,
        Instant sentAt,
        String senderNickname,
        String senderProfileIconUrl,
        String senderVoteOption,
        boolean isMine
) {
    public static MessageResponse from(MessageResult result) {
        return new MessageResponse(
                result.messageId(),
                result.content(),
                result.sentAt(),
                result.senderNickname(),
                result.senderProfileIconUrl(),
                result.senderVoteOption(),
                result.isMine()
        );
    }
}
