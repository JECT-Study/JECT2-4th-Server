package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.MessagePageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "채팅 메시지 페이지 응답")
public record MessagePageResponse(
        @Schema(description = "조회된 메시지 목록")
        List<MessageResponse> messages,

        @Schema(description = "다음 페이지 조회에 사용할 커서. 다음 페이지가 없으면 null입니다.", example = "96", nullable = true)
        Long nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {

    public static MessagePageResponse from(MessagePageResult result) {
        List<MessageResponse> responses = result.messages().stream()
                .map(MessageResponse::from)
                .toList();
        return new MessagePageResponse(responses, result.nextCursor(), result.hasNext());
    }
}
