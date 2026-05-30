package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.VoteType;

import java.time.Instant;
import java.util.List;

public record MyParticipatedVoteResponse(
        long count,
        List<VoteElement> voteList
) {
    public record VoteElement(
            Long id,
            VoteType voteType,
            String title,
            String content,
            String thumbnailUrl,
            Instant localDate,
            Instant endAt
    ) {}
}
