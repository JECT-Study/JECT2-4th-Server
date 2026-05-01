package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.ChatListItemResult;

import java.time.Instant;

public record ChatListItemResponse(
        Long voteId,
        String title,
        String thumbnailUrl,
        String optionA,
        String optionB,
        int participantCount,
        String lastMessage,
        Instant lastMessageAt,
        Instant endAt,
        int unreadCount
) {
    public static ChatListItemResponse from(ChatListItemResult result) {
        return new ChatListItemResponse(
                result.voteId(),
                result.title(),
                result.thumbnailUrl(),
                result.optionA(),
                result.optionB(),
                result.participantCount(),
                result.lastMessage(),
                result.lastMessageAt(),
                result.endAt(),
                result.unreadCount()
        );
    }
}
