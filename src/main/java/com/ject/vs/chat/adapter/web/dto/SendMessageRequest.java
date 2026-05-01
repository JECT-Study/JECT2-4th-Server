package com.ject.vs.chat.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "채팅 메시지 전송 요청")
public record SendMessageRequest(
        @Schema(description = "전송할 메시지 내용", example = "저는 A가 좋아요", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String content
) {}
