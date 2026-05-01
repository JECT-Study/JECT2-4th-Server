package com.ject.vs.chat.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "채팅방 읽음 처리 요청")
public record MarkAsReadRequest(
        @Schema(description = "현재 사용자가 마지막으로 읽은 메시지 ID", example = "128", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Long lastReadMessageId
) {}
