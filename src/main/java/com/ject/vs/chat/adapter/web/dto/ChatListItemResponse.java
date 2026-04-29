package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.ChatListItemResult;
import com.ject.vs.common.util.TimeUtils;

import java.time.OffsetDateTime;

public record ChatListItemResponse(
        Long voteId,
        String title,
        String thumbnailUrl,
        String optionA,
        String optionB,
        int participantCount,
        String lastMessage,
        OffsetDateTime lastMessageAt,
        OffsetDateTime endAt,
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
                TimeUtils.toKstOffsetDateTime(result.lastMessageAt()),
                TimeUtils.toKstOffsetDateTime(result.endAt()),
                result.unreadCount()
        );
    }
}
