package com.ject.vs.home.adapter.web.dto;

import com.ject.vs.home.port.in.HomeVoteQueryUseCase.HotTopicResult;
import com.ject.vs.vote.domain.VoteType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record HomeHotTopicResponse(List<HotTopicItem> hotTopics) {

    public record HotTopicItem(
            int rank,
            Long voteId,
            String thumbnailUrl,
            VoteType voteType,
            String title,
            String content,
            long participantCount,
            OffsetDateTime endAt
    ) {
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(9));
    }

    public static HomeHotTopicResponse from(HotTopicResult result) {
        List<HotTopicItem> items = result.items().stream()
                .map(i -> new HotTopicItem(
                        i.rank(),
                        i.voteId(),
                        i.thumbnailUrl(),
                        i.voteType(),
                        i.title(),
                        i.content(),
                        i.participantCount(),
                        toKst(i.endAt())
                ))
                .toList();
        return new HomeHotTopicResponse(items);
    }
}
