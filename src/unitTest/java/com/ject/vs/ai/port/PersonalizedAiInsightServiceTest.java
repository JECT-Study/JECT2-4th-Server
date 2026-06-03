package com.ject.vs.ai.port;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ject.vs.ai.port.in.AiInsightUseCase;
import com.ject.vs.ai.port.in.AiInsightUseCase.AiInsightResult;
import com.ject.vs.ai.port.in.AiInsightUseCase.PersonalizedVoteInsightRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalizedAiInsightServiceTest {

    @Mock
    PersonalizedInsightDataCollector dataCollector;

    @Mock
    AiInsightUseCase aiInsightUseCase;

    Cache<String, AiInsightResult> cache;

    PersonalizedAiInsightService service;

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder().maximumSize(100).build();
        service = new PersonalizedAiInsightService(dataCollector, aiInsightUseCase, cache);
    }

    PersonalizedVoteInsightRequest createRequest(String gender, String ageGroup) {
        return new PersonalizedVoteInsightRequest(
                "짜장면 vs 짬뽕",
                "짜장면", 60, 60,
                "짬뽕", 40, 40,
                100, 50, 50, "20s",
                "짜장면", gender, ageGroup,
                65, 70, "짜장면", "짜장면"
        );
    }

    @Nested
    @DisplayName("getOrGenerate")
    class GetOrGenerate {

        @Test
        @DisplayName("캐시 미스 시 AI 호출하고 결과 캐싱")
        void cachesMissCallsAiAndCachesResult() {
            // given
            Long voteId = 1L, userId = 100L, optionId = 10L;
            PersonalizedVoteInsightRequest request = createRequest("MALE", "20s");
            AiInsightResult aiResult = new AiInsightResult("당신은 다수파!", "남성 20대의 65%가 동일 선택");

            given(dataCollector.collect(voteId, userId, optionId)).willReturn(request);
            given(aiInsightUseCase.generatePersonalizedInsight(request)).willReturn(Optional.of(aiResult));

            // when
            Optional<AiInsightResult> result = service.getOrGenerate(voteId, userId, optionId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().headline()).isEqualTo("당신은 다수파!");
            assertThat(result.get().body()).isEqualTo("남성 20대의 65%가 동일 선택");

            // 캐시에 저장됨
            String cacheKey = request.cacheKey(voteId, userId, optionId);
            assertThat(cache.getIfPresent(cacheKey)).isNotNull();
        }

        @Test
        @DisplayName("캐시 히트 시 AI 호출 안함")
        void cacheHitDoesNotCallAi() {
            // given
            Long voteId = 1L, userId = 100L, optionId = 10L;
            PersonalizedVoteInsightRequest request = createRequest("FEMALE", "30s");
            AiInsightResult cachedResult = new AiInsightResult("캐시된 헤드라인", "캐시된 바디");

            String cacheKey = request.cacheKey(voteId, userId, optionId);
            cache.put(cacheKey, cachedResult);

            given(dataCollector.collect(voteId, userId, optionId)).willReturn(request);

            // when
            Optional<AiInsightResult> result = service.getOrGenerate(voteId, userId, optionId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().headline()).isEqualTo("캐시된 헤드라인");

            // AI 호출 안함
            verify(aiInsightUseCase, never()).generatePersonalizedInsight(any());
        }

        @Test
        @DisplayName("AI 호출 실패 시 빈 결과 반환, 캐시 안함")
        void aiFailureReturnsEmptyAndDoesNotCache() {
            // given
            Long voteId = 1L, userId = 100L, optionId = 10L;
            PersonalizedVoteInsightRequest request = createRequest("MALE", "40s");

            given(dataCollector.collect(voteId, userId, optionId)).willReturn(request);
            given(aiInsightUseCase.generatePersonalizedInsight(request)).willReturn(Optional.empty());

            // when
            Optional<AiInsightResult> result = service.getOrGenerate(voteId, userId, optionId);

            // then
            assertThat(result).isEmpty();

            // 캐시에 저장 안됨
            String cacheKey = request.cacheKey(voteId, userId, optionId);
            assertThat(cache.getIfPresent(cacheKey)).isNull();
        }

        @Test
        @DisplayName("다른 성별/연령대는 다른 캐시 키")
        void differentDemographicsUseDifferentCacheKeys() {
            // given
            Long voteId = 1L, optionId = 10L;

            PersonalizedVoteInsightRequest maleRequest = createRequest("MALE", "20s");
            PersonalizedVoteInsightRequest femaleRequest = createRequest("FEMALE", "20s");

            AiInsightResult maleResult = new AiInsightResult("남성 인사이트", "남성 바디");
            AiInsightResult femaleResult = new AiInsightResult("여성 인사이트", "여성 바디");

            given(dataCollector.collect(voteId, 100L, optionId)).willReturn(maleRequest);
            given(dataCollector.collect(voteId, 200L, optionId)).willReturn(femaleRequest);
            given(aiInsightUseCase.generatePersonalizedInsight(maleRequest)).willReturn(Optional.of(maleResult));
            given(aiInsightUseCase.generatePersonalizedInsight(femaleRequest)).willReturn(Optional.of(femaleResult));

            // when
            Optional<AiInsightResult> maleResultActual = service.getOrGenerate(voteId, 100L, optionId);
            Optional<AiInsightResult> femaleResultActual = service.getOrGenerate(voteId, 200L, optionId);

            // then
            assertThat(maleResultActual.get().headline()).isEqualTo("남성 인사이트");
            assertThat(femaleResultActual.get().headline()).isEqualTo("여성 인사이트");

            // 둘 다 AI 호출됨 (캐시 키가 다름)
            verify(aiInsightUseCase, times(2)).generatePersonalizedInsight(any());
        }
    }
}
