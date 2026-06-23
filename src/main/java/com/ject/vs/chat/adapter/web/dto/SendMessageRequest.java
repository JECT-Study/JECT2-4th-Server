package com.ject.vs.chat.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "채팅 메시지 전송 요청")
public record SendMessageRequest(
        @Schema(description = "전송할 메시지 내용", example = "저는 A가 좋아요", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String content,

        @Schema(description = "답글 대상 메시지 ID (없으면 일반 메시지)", example = "1234", nullable = true)
        Long replyToMessageId
) {
    public SendMessageRequest(String content) {
        this(content, null);
    }
}
