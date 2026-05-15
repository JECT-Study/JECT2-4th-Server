package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveLiveResult;

public record ImmersiveLiveResponse(
        Long voteId,
        int optionARatio,
        int optionBRatio,
        int participantCount,
        int currentViewerCount
) {
    public static ImmersiveLiveResponse from(ImmersiveLiveResult result) {
        return new ImmersiveLiveResponse(result.voteId(), result.optionARatio(), result.optionBRatio(),
                result.participantCount(), result.currentViewerCount());
    }
}
