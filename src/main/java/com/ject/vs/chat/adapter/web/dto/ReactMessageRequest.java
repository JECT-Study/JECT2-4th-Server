package com.ject.vs.chat.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "메시지 반응 요청")
public record ReactMessageRequest(
        @Schema(description = "반응 이모지 (THUMBS_UP, THUMBS_DOWN 또는 null=취소)", example = "THUMBS_UP", nullable = true)
        String emoji
) {}
