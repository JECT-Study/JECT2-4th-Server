package com.ject.vs.vote.port.in;

import com.ject.vs.vote.domain.InsightScope;
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
            Long mySelectedOptionId,
            Insight insight,
            AiInsightView aiInsight
    ) {
    }

    record Insight(
            boolean locked,
            InsightScope scope,
            Integer selectionCount,
            GenderDistribution genderDistribution,
            List<AgeDistribution> ageDistribution
    ) {
        public static Insight ofLocked() {
            return new Insight(true, null, null, null, null);
        }
    }

    record GenderDistribution(int maleRatio, int femaleRatio) {
    }

    record AgeDistribution(String ageGroup, int ratio, boolean isMyGroup) {
    }

    record AiInsightView(boolean available, String headline, String body) {
        public static AiInsightView of(String headline, String body) {
            return new AiInsightView(true, headline, body);
        }

        public static AiInsightView unavailable() {
            return new AiInsightView(false, null, null);
        }
    }

    record ShareLinkResult(String url) {
    }
}
