package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveLiveResult;

import java.util.List;

public record ImmersiveLiveResponse(
        List<OptionItem> options,
        int currentViewerCount,
        int totalParticipantCount
) {
    public record OptionItem(Long optionId, long voteCount, int ratio) {
    }

    public static ImmersiveLiveResponse from(ImmersiveLiveResult result) {
        List<OptionItem> options = result.options().stream()
                .map(o -> new OptionItem(o.optionId(), o.voteCount(), o.ratio()))
                .toList();
        return new ImmersiveLiveResponse(options, result.currentViewerCount(), result.totalParticipantCount());
    }
}
