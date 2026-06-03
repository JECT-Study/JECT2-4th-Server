package com.ject.vs.ai.port;

import com.github.benmanes.caffeine.cache.Cache;
import com.ject.vs.ai.port.in.AiInsightUseCase;
import com.ject.vs.ai.port.in.AiInsightUseCase.AiInsightResult;
import com.ject.vs.ai.port.in.AiInsightUseCase.PersonalizedVoteInsightRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizedAiInsightService {

    private final PersonalizedInsightDataCollector dataCollector;
    private final AiInsightUseCase aiInsightUseCase;
    private final Cache<String, AiInsightResult> personalizedAiInsightCache;

    public Optional<AiInsightResult> getOrGenerate(Long voteId, Long userId, Long selectedOptionId) {
        PersonalizedVoteInsightRequest request = dataCollector.collect(voteId, userId, selectedOptionId);
        String cacheKey = request.cacheKey(voteId, userId, selectedOptionId);

        AiInsightResult cached = personalizedAiInsightCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for personalized AI insight: {}", cacheKey);
            return Optional.of(cached);
        }

        log.debug("Cache miss for personalized AI insight: {}, generating...", cacheKey);
        Optional<AiInsightResult> result = aiInsightUseCase.generatePersonalizedInsight(request);

        result.ifPresent(insight -> {
            personalizedAiInsightCache.put(cacheKey, insight);
            log.info("Personalized AI insight generated and cached for vote: {}, user: {}", voteId, userId);
        });

        return result;
    }
}
