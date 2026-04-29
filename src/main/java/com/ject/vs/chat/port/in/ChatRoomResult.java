package com.ject.vs.chat.port.in;

import java.time.LocalDateTime;

public record ChatRoomResult(
        Long voteId,
        String title,
        VoteStatus status,
        int participantCount,
        String optionA,
        String optionB,
        LocalDateTime endAt
) {}
