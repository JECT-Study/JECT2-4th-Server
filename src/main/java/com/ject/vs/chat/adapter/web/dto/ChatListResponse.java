package com.ject.vs.chat.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "채팅방 목록 응답")
public record ChatListResponse(
        @Schema(description = "조회된 채팅방 목록")
        List<ChatListItemResponse> chats
) {}
