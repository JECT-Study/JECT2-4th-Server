package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.MessagePageResult;

import java.util.List;

public record MessagePageResponse(List<MessageResponse> messages, Long nextCursor, boolean hasNext) {

    public static MessagePageResponse from(MessagePageResult result) {
        List<MessageResponse> responses = result.messages().stream()
                .map(MessageResponse::from)
                .toList();
        return new MessagePageResponse(responses, result.nextCursor(), result.hasNext());
    }
}
