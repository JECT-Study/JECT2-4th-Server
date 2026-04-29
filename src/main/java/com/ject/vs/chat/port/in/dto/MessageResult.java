package com.ject.vs.chat.port.in.dto;

import java.time.Instant;

public record MessageResult(
        Long messageId,
        String content,
        Instant sentAt,
        String senderNickname,
        String senderProfileIconUrl,
        String senderVoteOption,
        boolean isMine
) {}
