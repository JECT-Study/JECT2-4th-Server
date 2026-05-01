package com.ject.vs.chat.port.in.dto;

import java.time.Instant;

public record ChatListItemResult(
        Long voteId,
        String title,           // TODO: Vote 도메인 연동 후 채워야 함
        String thumbnailUrl,    // TODO: Vote 도메인 연동 후 채워야 함
        String optionA,         // TODO: Vote 도메인 연동 후 채워야 함
        String optionB,         // TODO: Vote 도메인 연동 후 채워야 함
        int participantCount,
        String lastMessage,
        Instant lastMessageAt,
        Instant endAt,          // TODO: Vote 도메인 연동 후 채워야 함
        int unreadCount
) {
    public static ChatListItemResult of(
            Long voteId,
            int participantCount,
            String lastMessage,
            Instant lastMessageAt,
            int unreadCount
    ) {
        return new ChatListItemResult(
                voteId,
                null,   // TODO: Vote 도메인 연동 후 채워야 함
                null,   // TODO: Vote 도메인 연동 후 채워야 함
                null,   // TODO: Vote 도메인 연동 후 채워야 함
                null,   // TODO: Vote 도메인 연동 후 채워야 함
                participantCount,
                lastMessage,
                lastMessageAt,
                null,   // TODO: Vote 도메인 연동 후 채워야 함
                unreadCount
        );
    }
}
