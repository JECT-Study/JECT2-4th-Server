package com.ject.vs.vote.adapter.web.dto;

import java.time.Instant;
import java.util.List;

public record MyParticipatedVoteResponse(
        long count,
        List<VoteElement> voteList
) {
    public record VoteElement(
            Long id,
            String title,
            String content,
            String thumbnailUrl,
            Instant localDate,
            Instant endAt
    ) {}
}
