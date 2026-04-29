package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.MessageResult;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record MessageResponse(
        Long messageId,
        String content,
        OffsetDateTime sentAt,
        String senderNickname,
        String senderProfileIconUrl,
        String senderVoteOption,
        boolean isMine
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static MessageResponse from(MessageResult result) {
        return new MessageResponse(
                result.messageId(),
                result.content(),
                toOffsetDateTime(result.sentAt()),
                result.senderNickname(),
                result.senderProfileIconUrl(),
                result.senderVoteOption(),
                result.isMine()
        );
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(KST).toOffsetDateTime();
    }
}
