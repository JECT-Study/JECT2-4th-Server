package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.ChatRoomResult;
import com.ject.vs.common.util.TimeUtils;
import com.ject.vs.vote.port.in.dto.VoteStatus;

import java.time.OffsetDateTime;

public record ChatRoomResponse(
        Long voteId,
        String title,
        VoteStatus status,
        int participantCount,
        String optionA,
        String optionB,
        OffsetDateTime endAt
) {
    public static ChatRoomResponse from(ChatRoomResult result) {
        return new ChatRoomResponse(
                result.voteId(),
                result.title(),
                result.status(),
                result.participantCount(),
                result.optionA(),
                result.optionB(),
                TimeUtils.toKstOffsetDateTime(result.endAt())
        );
    }
}
