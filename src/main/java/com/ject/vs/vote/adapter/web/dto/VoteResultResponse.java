package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.port.in.VoteResultQueryUseCase.VoteResultDetail;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record VoteResultResponse(
        Long voteId,
        String title,
        String status,
        OffsetDateTime endAt,
        int participantCount,
        List<OptionItem> options,
        Long mySelectedOptionId
) {
    public record OptionItem(Long optionId, String label, long voteCount, Integer ratio) {
    }

    private static OffsetDateTime toKst(Instant instant) {
        return instant.atOffset(ZoneOffset.ofHours(9));
    }

    public static VoteResultResponse from(VoteResultDetail result) {
        List<OptionItem> items = result.options().stream()
                .map(o -> new OptionItem(o.optionId(), o.label(), o.voteCount(), o.ratio()))
                .toList();
        return new VoteResultResponse(result.voteId(), result.title(), result.status().name(),
                toKst(result.endAt()), result.participantCount(), items, result.mySelectedOptionId());
    }
}
