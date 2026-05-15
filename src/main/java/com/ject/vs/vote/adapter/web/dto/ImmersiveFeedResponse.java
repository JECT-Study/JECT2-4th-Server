package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveFeedResult;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record ImmersiveFeedResponse(List<FeedItem> items, Long nextCursor, boolean hasNext) {

    public record FeedItem(
            Long voteId,
            String title,
            String imageUrl,
            String status,
            OffsetDateTime endAt,
            int participantCount,
            int currentViewerCount,
            Long mySelectedOptionId
    ) {
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(9));
    }

    public static ImmersiveFeedResponse from(ImmersiveFeedResult result) {
        List<FeedItem> items = result.items().stream()
                .map(i -> new FeedItem(
                        i.voteId(), i.title(), i.imageUrl(), i.status().name(),
                        toKst(i.endAt()), i.participantCount(), i.currentViewerCount(),
                        i.mySelectedOptionId()))
                .toList();
        return new ImmersiveFeedResponse(items, result.nextCursor(), result.hasNext());
    }
}
