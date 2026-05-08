package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.VoteEmoji;
import com.ject.vs.vote.port.VoteDetailQueryService.VoteDetailResult;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record VoteDetailResponse(
        Long voteId,
        String type,
        String title,
        String content,
        String thumbnailUrl,
        String imageUrl,
        String status,
        OffsetDateTime endAt,
        int participantCount,
        List<OptionItem> options,
        Long mySelectedOptionId,
        Map<String, Long> emojiSummary,
        String myEmoji
) {
    public record OptionItem(Long optionId, String label, long voteCount, Integer ratio) {
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(9));
    }

    public static VoteDetailResponse from(VoteDetailResult result) {
        List<OptionItem> items = result.options().stream()
                .map(o -> new OptionItem(o.optionId(), o.label(), o.voteCount(), o.ratio()))
                .toList();
        Map<String, Long> summary = result.emojiSummary().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
        String myEmoji = result.myEmoji() != null ? result.myEmoji().name() : null;
        return new VoteDetailResponse(
                result.voteId(), result.type().name(), result.title(), result.content(),
                result.thumbnailUrl(), result.imageUrl(), result.status().name(),
                toKst(result.endAt()), result.participantCount(), items,
                result.mySelectedOptionId(), summary, myEmoji
        );
    }
}
