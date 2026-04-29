package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.ChatRoomResult;
import com.ject.vs.chat.port.in.VoteStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record ChatRoomResponse(
        Long voteId,
        String title,
        VoteStatus status,
        int participantCount,
        String optionA,
        String optionB,
        OffsetDateTime endAt
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static ChatRoomResponse from(ChatRoomResult result) {
        return new ChatRoomResponse(
                result.voteId(),
                result.title(),
                result.status(),
                result.participantCount(),
                result.optionA(),
                result.optionB(),
                toOffsetDateTime(result.endAt())
        );
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(KST).toOffsetDateTime();
    }
}
