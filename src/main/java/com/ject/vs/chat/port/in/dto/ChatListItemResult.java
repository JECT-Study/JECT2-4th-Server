package com.ject.vs.chat.port.in.dto;

import com.ject.vs.vote.port.in.VoteQueryUseCase.VoteChatSummary;

import java.time.Instant;

public record ChatListItemResult(
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
    public static ChatListItemResult of(
            VoteChatSummary vote,
            int participantCount,
            String lastMessage,
            Instant lastMessageAt,
            int unreadCount
    ) {
        return new ChatListItemResult(
                vote.voteId(),
                vote.title(),
                vote.thumbnailUrl(),
                vote.optionA(),
                vote.optionB(),
                participantCount,
                lastMessage,
                lastMessageAt,
                vote.endAt(),
                unreadCount
        );
    }
}
