package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.port.in.VoteCommandUseCase.ParticipateResult;

import java.util.List;

public record ParticipateResponse(
        Long voteId,
        Long selectedOptionId,
        List<OptionItem> options,
        int participantCount,
        Integer remainingFreeVotes
) {
    public record OptionItem(Long optionId, String label, long voteCount, Integer ratio) {
    }

    public static ParticipateResponse from(ParticipateResult result) {
        List<OptionItem> items = result.options().stream()
                .map(o -> new OptionItem(o.optionId(), o.label(), o.voteCount(), o.ratio()))
                .toList();
        return new ParticipateResponse(result.voteId(), result.selectedOptionId(),
                items, result.participantCount(), result.remainingFreeVotes());
    }
}
