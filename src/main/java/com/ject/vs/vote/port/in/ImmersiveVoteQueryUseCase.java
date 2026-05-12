package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.VoteStatus;

import java.time.Instant;
import java.util.List;

public interface ImmersiveVoteQueryUseCase {

    ImmersiveFeedResult getFeed(Long cursor, int size, Long userId, String anonymousId);

    ImmersiveLiveResult getLive(Long voteId);

    record ImmersiveFeedResult(List<ImmersiveFeedItem> items, Long nextCursor, boolean hasNext) {
    }

    record ImmersiveFeedItem(
            Long voteId,
            String title,
            String imageUrl,
            VoteStatus status,
            Instant endAt,
            int participantCount,
            int currentViewerCount,
            Long mySelectedOptionId
    ) {
    }

    record ImmersiveLiveResult(
            Long voteId,
            int optionARatio,
            int optionBRatio,
            int participantCount,
            int currentViewerCount
    ) {
    }
}
