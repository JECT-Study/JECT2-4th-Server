package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.domain.ChatReactionType;
import com.ject.vs.chat.port.in.dto.ReactionResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "메시지 반응 응답")
public record ReactionResponse(
        @Schema(description = "메시지 ID")
        Long messageId,

        @Schema(description = "반응 카운트")
        Map<ChatReactionType, Long> reactions,

        @Schema(description = "내 반응", nullable = true)
        ChatReactionType myReaction
) {
    public static ReactionResponse from(ReactionResult result) {
        return new ReactionResponse(
                result.messageId(),
                result.reactions() != null ? result.reactions() : Map.of(),
                result.myReaction()
        );
    }
}
