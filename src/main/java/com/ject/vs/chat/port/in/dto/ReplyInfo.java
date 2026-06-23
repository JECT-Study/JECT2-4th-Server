package com.ject.vs.chat.port.in.dto;

public record ReplyInfo(
        Long messageId,
        String senderNickname,
        String contentPreview
) {}
