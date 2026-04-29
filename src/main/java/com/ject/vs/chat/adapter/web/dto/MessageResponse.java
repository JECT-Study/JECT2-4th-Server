package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.common.util.TimeUtils;

import java.time.OffsetDateTime;

public record MessageResponse(
        Long messageId,
        String content,
        OffsetDateTime sentAt,
        String senderNickname,
        String senderProfileIconUrl,
        String senderVoteOption,
        boolean isMine
) {
    public static MessageResponse from(MessageResult result) {
        return new MessageResponse(
                result.messageId(),
                result.content(),
                TimeUtils.toKstOffsetDateTime(result.sentAt()),
                result.senderNickname(),
                result.senderProfileIconUrl(),
                result.senderVoteOption(),
                result.isMine()
        );
    }
}
