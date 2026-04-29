package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.ChatListItemResult;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static ChatListItemResponse from(ChatListItemResult result) {
        return new ChatListItemResponse(
                result.voteId(),
                result.title(),
                result.thumbnailUrl(),
                result.optionA(),
                result.optionB(),
                result.participantCount(),
                result.lastMessage(),
                toOffsetDateTime(result.lastMessageAt()),
                toOffsetDateTime(result.endAt()),
                result.unreadCount()
        );
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(KST).toOffsetDateTime();
    }
}
