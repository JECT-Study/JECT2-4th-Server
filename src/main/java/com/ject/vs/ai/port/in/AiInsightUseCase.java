package com.ject.vs.ai.port.in;

import java.util.Optional;

public interface AiInsightUseCase {

    Optional<AiInsightResult> generateVoteInsight(VoteInsightRequest request);

    Optional<AiInsightResult> generatePersonalizedInsight(PersonalizedVoteInsightRequest request);

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

    record PersonalizedVoteInsightRequest(
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
            String majorityAgeGroup,
            String userSelectedOption,
            String userGender,
            String userAgeGroup,
            int sameGenderRatio,
            int sameAgeGroupRatio,
            String sameGenderMajorityOption,
            String sameAgeGroupMajorityOption
    ) {
        public String cacheKey(Long voteId, Long userId, Long selectedOptionId) {
            return String.format("%d:%d:%d:%s:%s",
                    voteId, userId, selectedOptionId, userGender, userAgeGroup);
        }
    }

    record AiInsightResult(
            String headline,
            String body
    ) {
    }
}
