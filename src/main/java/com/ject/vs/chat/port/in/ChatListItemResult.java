package com.ject.vs.chat.port.in;

import java.time.LocalDateTime;

public record ChatListItemResult(
        Long voteId,
        String title,
        String thumbnailUrl,
        String optionA,
        String optionB,
        int participantCount,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime endAt,
        int unreadCount
) {}
