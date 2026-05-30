package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.VoteEmoji;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ImmersiveVoteQueryUseCase {

    ImmersiveFeedResult getFeed(Long cursor, Long startVoteId, int size, Long userId, String anonymousId);

    ImmersiveLiveResult getLive(Long voteId);

    record ImmersiveFeedResult(List<ImmersiveFeedItem> items, Long nextCursor, boolean hasNext) {
    }

    record ImmersiveFeedItem(
            Long voteId,
            String title,
            String content,
            String imageUrl,
            Instant endAt,
            List<FeedOptionItem> options,
            boolean voted,
            Long mySelectedOptionId,
            Map<VoteEmoji, Long> emojiSummary,
            long emojiTotal,
            VoteEmoji myEmoji,
            int commentCount,
            int currentViewerCount
    ) {
    }

    record FeedOptionItem(Long optionId, String label, Long voteCount, Integer ratio) {
    }

    record ImmersiveLiveResult(
            List<LiveOptionItem> options,
            int currentViewerCount,
            int totalParticipantCount
    ) {
    }

    record LiveOptionItem(Long optionId, long voteCount, int ratio) {
    }
}
