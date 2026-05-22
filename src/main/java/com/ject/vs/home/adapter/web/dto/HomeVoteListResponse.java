package com.ject.vs.home.adapter.web.dto;

import com.ject.vs.home.port.in.HomeVoteQueryUseCase.VoteListResult;
import com.ject.vs.vote.domain.VoteStatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record HomeVoteListResponse(
        List<VoteListItem> votes,
        Long nextCursor,
        boolean hasNext
) {

    public record VoteListItem(
            Long voteId,
            String thumbnailUrl,
            VoteStatus status,
            String title,
            String content,
            OffsetDateTime endAt
    ) {
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(9));
    }

    public static HomeVoteListResponse from(VoteListResult result) {
        List<VoteListItem> items = result.items().stream()
                .map(i -> new VoteListItem(
                        i.voteId(),
                        i.thumbnailUrl(),
                        i.status(),
                        i.title(),
                        i.content(),
                        toKst(i.endAt())
                ))
                .toList();
        return new HomeVoteListResponse(items, result.nextCursor(), result.hasNext());
    }
}
