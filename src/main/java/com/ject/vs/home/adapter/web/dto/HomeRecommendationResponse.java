package com.ject.vs.home.adapter.web.dto;

import com.ject.vs.home.port.in.HomeVoteQueryUseCase.RecommendationResult;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record HomeRecommendationResponse(List<RecommendationItem> recommendations) {

    public record RecommendationItem(
            Long voteId,
            String thumbnailUrl,
            String title,
            String content,
            OffsetDateTime endAt
    ) {
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(9));
    }

    public static HomeRecommendationResponse from(RecommendationResult result) {
        List<RecommendationItem> items = result.items().stream()
                .map(i -> new RecommendationItem(
                        i.voteId(),
                        i.thumbnailUrl(),
                        i.title(),
                        i.content(),
                        toKst(i.endAt())
                ))
                .toList();
        return new HomeRecommendationResponse(items);
    }
}
