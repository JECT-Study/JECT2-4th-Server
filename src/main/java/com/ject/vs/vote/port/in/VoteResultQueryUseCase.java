package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.VoteStatus;

import java.time.Instant;
import java.util.List;

public interface VoteResultQueryUseCase {

    VoteResultDetail getResult(Long voteId, Long userId);

    ShareLinkResult getShareLink(Long voteId);

    record VoteResultDetail(
            Long voteId,
            String title,
            VoteStatus status,
            Instant endAt,
            int participantCount,
            List<VoteCommandUseCase.OptionResult> options,
            Long mySelectedOptionId
    ) {
    }

    record ShareLinkResult(String url) {
    }
}
