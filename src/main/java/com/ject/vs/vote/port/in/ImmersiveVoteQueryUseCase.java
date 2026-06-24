package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.VoteEmoji;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ImmersiveVoteQueryUseCase {

    ImmersiveFeedResult getFeed(Long cursor, Long startVoteId, int size, Long userId, String anonymousId);

    ImmersiveLiveResult getLive(Long voteId);

    /**
     * 랜덤 다음 투표 조회 (excludeIds 제외, 무한 순환)
     * startVoteId가 지정되면 진행 중인 해당 투표를 맨 앞에 배치하고 나머지를 랜덤으로 채운다.
     */
    ImmersiveNextResult getNextRandom(List<Long> excludeIds, Long startVoteId, int size, Long userId, String anonymousId);

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

    /**
     * 랜덤 다음 투표 조회 결과 (무한 순환용)
     */
    record ImmersiveNextResult(List<ImmersiveFeedItem> items) {
    }
}
