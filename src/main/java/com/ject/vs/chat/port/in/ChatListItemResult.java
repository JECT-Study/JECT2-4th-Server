package com.ject.vs.chat.port.in;

import java.time.LocalDateTime;

public record ChatListItemResult(
        Long voteId,
        String title,           // TODO: Vote 도메인 연동 후 채워야 함
        String thumbnailUrl,    // TODO: Vote 도메인 연동 후 채워야 함
        String optionA,         // TODO: Vote 도메인 연동 후 채워야 함
        String optionB,         // TODO: Vote 도메인 연동 후 채워야 함
        int participantCount,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime endAt,    // TODO: Vote 도메인 연동 후 채워야 함
        int unreadCount
) {
    public static ChatListItemResult of(
            Long voteId,
            int participantCount,
            String lastMessage,
            LocalDateTime lastMessageAt,
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
