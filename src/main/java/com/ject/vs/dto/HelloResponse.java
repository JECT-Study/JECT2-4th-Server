package com.ject.vs.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Hello API 응답")
public record HelloResponse(
        @Schema(description = "인사 메시지", example = "Hello, VS Server!")
        String message,

        @Schema(description = "서버 타임스탬프", example = "2026-03-14T12:00:00")
        String timestamp
) {
}
