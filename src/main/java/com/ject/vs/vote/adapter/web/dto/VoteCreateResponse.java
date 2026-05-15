package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.port.in.VoteCommandUseCase.VoteCreateResult;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record VoteCreateResponse(Long voteId, String status, OffsetDateTime endAt) {

    private static OffsetDateTime toKst(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(9));
    }

    public static VoteCreateResponse from(VoteCreateResult result) {
        return new VoteCreateResponse(result.voteId(), result.status().name(), toKst(result.endAt()));
    }
}
