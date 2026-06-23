package com.ject.vs.chat.port.in.dto;

public record ReplyInfo(
        Long messageId,
        String senderNickname,
        String contentPreview   // 부모 메시지 전체 내용 (트렁케이션은 프론트에서 처리)
) {}
