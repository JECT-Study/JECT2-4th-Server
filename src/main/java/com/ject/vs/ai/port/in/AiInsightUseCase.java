package com.ject.vs.ai.port.in;

import java.util.Optional;

public interface AiInsightUseCase {

    Optional<AiInsightResult> generateVoteInsight(VoteInsightRequest request);

    record VoteInsightRequest(
            String voteTitle,
            String optionALabel,
            long optionACount,
            int optionARatio,
            String optionBLabel,
            long optionBCount,
            int optionBRatio,
            long totalParticipants,
            int femaleRatio,
            int maleRatio,
            String majorityAgeGroup
    ) {
    }

    record AiInsightResult(
            String headline,
            String body
    ) {
    }
}
