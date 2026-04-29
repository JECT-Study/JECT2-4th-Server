package com.ject.vs.chat.port.in.dto;

import java.time.LocalDateTime;

public record ChatRoomResult(
        Long voteId,
        String title,       // TODO: Vote 도메인 연동 후 채워야 함
        VoteStatus status,  // TODO: Vote 도메인 연동 후 채워야 함
        int participantCount,
        String optionA,     // TODO: Vote 도메인 연동 후 채워야 함
        String optionB,     // TODO: Vote 도메인 연동 후 채워야 함
        LocalDateTime endAt // TODO: Vote 도메인 연동 후 채워야 함
) {
    public static ChatRoomResult of(Long voteId, int participantCount) {
        return new ChatRoomResult(
                voteId,
                null,   // TODO: Vote 도메인 연동 후 채워야 함
                null,   // TODO: Vote 도메인 연동 후 채워야 함
                participantCount,
                null,   // TODO: Vote 도메인 연동 후 채워야 함
                null,   // TODO: Vote 도메인 연동 후 채워야 함
                null    // TODO: Vote 도메인 연동 후 채워야 함
        );
    }
}
