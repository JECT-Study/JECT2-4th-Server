package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.domain.ChatReactionType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메시지 반응 요청")
public record ReactMessageRequest(
        @Schema(
                description = "반응 이모지. null을 보내면 기존 반응 취소",
                example = "THUMBS_UP",
                allowableValues = {"THUMBS_UP", "THUMBS_DOWN"},
                nullable = true
        )
        ChatReactionType emoji
) {}
