package com.ject.vs.chat.port.in;

import java.time.LocalDateTime;

public record MessageResult(
        Long messageId,
        String content,
        LocalDateTime sentAt,
        String senderNickname,
        String senderProfileIconUrl,
        String senderVoteOption,
        boolean isMine
) {}
