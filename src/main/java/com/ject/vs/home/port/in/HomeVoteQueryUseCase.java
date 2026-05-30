package com.ject.vs.home.port.in;

import com.ject.vs.vote.domain.VoteStatus;
import com.ject.vs.vote.domain.VoteType;

import java.time.Instant;
import java.util.List;

public interface HomeVoteQueryUseCase {

    RecommendationResult getRecommendations();

    HotTopicResult getHotTopics();

    VoteListResult getVoteList(String cursor, int size, VoteSortType sortType, boolean excludeEnded);

    enum VoteSortType {
        LATEST,     // 최신순 (생성일 기준)
        POPULAR,    // 인기순 (조회수 기준)
        ENDING_SOON // 종료임박순
    }

    // 오늘의 추천 결과
    record RecommendationResult(List<RecommendationItem> items) {
    }

    record RecommendationItem(
            Long voteId,
            String thumbnailUrl,
            VoteType voteType,
            String title,
            String content,
            Instant endAt
    ) {
    }

    // 핫토픽 TOP 3 결과
    record HotTopicResult(List<HotTopicItem> items) {
    }

    record HotTopicItem(
            int rank,
            Long voteId,
            String thumbnailUrl,
            VoteType voteType,
            String title,
            String content,
            long participantCount,
            Instant endAt
    ) {
    }

    // 전체 투표 목록 결과
    record VoteListResult(
            List<VoteListItem> items,
            String nextCursor,
            boolean hasNext
    ) {
    }

    record VoteListItem(
            Long voteId,
            String thumbnailUrl,
            VoteStatus status,
            VoteType voteType,
            String title,
            String content,
            Instant endAt
    ) {
    }
}
