package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.port.in.ImmersiveVoteCommandUseCase.ImmersiveParticipateResult;

import java.util.List;

public record ImmersiveParticipateResponse(
        Long voteId,
        String action,
        Long selectedOptionId,
        List<OptionItem> options,
        Integer remainingFreeVotes
) {
    public record OptionItem(Long optionId, String label, long voteCount, Integer ratio) {
    }

    public static ImmersiveParticipateResponse from(ImmersiveParticipateResult result) {
        List<OptionItem> items = result.options().stream()
                .map(o -> new OptionItem(o.optionId(), o.label(), o.voteCount(), o.ratio()))
                .toList();
        return new ImmersiveParticipateResponse(
                result.voteId(), result.action().name(), result.selectedOptionId(),
                items, result.remainingFreeVotes()
        );
    }
}
