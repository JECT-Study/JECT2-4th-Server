package com.ject.vs.ai.port;

import com.ject.vs.ai.port.in.AiInsightUseCase;
import com.ject.vs.ai.port.in.AiInsightUseCase.AiInsightResult;
import com.ject.vs.ai.port.in.AiInsightUseCase.PersonalizedVoteInsightRequest;
import com.ject.vs.support.BaseIntegrationTest;
import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.VoteResultDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("개인화 AI 인사이트 통합 테스트")
class PersonalizedAiInsightIntegrationTest extends BaseIntegrationTest {

    @Autowired
    VoteRepository voteRepository;

    @Autowired
    VoteOptionRepository voteOptionRepository;

    @Autowired
    VoteParticipationRepository voteParticipationRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    VoteResultQueryUseCase voteResultQueryUseCase;

    @Autowired
    PersonalizedInsightDataCollector dataCollector;

    @MockitoBean
    AiInsightUseCase aiInsightUseCase;

    Vote vote;
    VoteOption optionA;
    VoteOption optionB;
    User maleUser20s;
    User femaleUser30s;
    User maleUser30s;

    @BeforeEach
    void setUpVoteAndUsers() {
        // 종료된 투표 생성
        vote = Vote.create(
                "짜장면 vs 짬뽕",
                "중식 선호도 조사",
                "https://example.com/thumb.jpg",
                null,
                Duration.ofHours(24),
                clock
        );
        vote = voteRepository.save(vote);

        // 투표 종료 처리 (endAt을 과거로)
        entityManager.createQuery("UPDATE Vote v SET v.endAt = :past WHERE v.id = :id")
                .setParameter("past", FIXED_NOW.minus(Duration.ofDays(1)))
                .setParameter("id", vote.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        vote = voteRepository.findById(vote.getId()).orElseThrow();

        // 옵션 생성
        optionA = VoteOption.of(vote, "짜장면", 0);
        optionB = VoteOption.of(vote, "짬뽕", 1);
        optionA = voteOptionRepository.save(optionA);
        optionB = voteOptionRepository.save(optionB);

        // 사용자 생성
        maleUser20s = createUser("male20s@test.com", Gender.MALE, Year.of(2000));  // 25세 → 20대
        femaleUser30s = createUser("female30s@test.com", Gender.FEMALE, Year.of(1992)); // 33세 → 30대
        maleUser30s = createUser("male30s@test.com", Gender.MALE, Year.of(1990)); // 35세 → 30대
    }

    User createUser(String email, Gender gender, Year birthYear) {
        User user = User.createWithEmail(email);
        user = userRepository.save(user);

        entityManager.createQuery(
                        "UPDATE User u SET u.gender = :gender, u.birthYear = :birthYear WHERE u.id = :id")
                .setParameter("gender", gender)
                .setParameter("birthYear", birthYear)
                .setParameter("id", user.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        return userRepository.findById(user.getId()).orElseThrow();
    }

    void participate(User user, VoteOption option) {
        VoteParticipation participation = VoteParticipation.ofMember(
                vote.getId(), user.getId(), option.getId());
        voteParticipationRepository.save(participation);
        entityManager.flush();
    }

    @Nested
    @DisplayName("PersonalizedInsightDataCollector")
    class DataCollectorTest {

        @Test
        @DisplayName("투표 기본 통계를 정확하게 수집한다")
        void collectsBasicVoteStatistics() {
            // given: 3명 참여 - 짜장면 2명, 짬뽕 1명
            participate(maleUser20s, optionA);
            participate(femaleUser30s, optionA);
            participate(maleUser30s, optionB);

            // when
            PersonalizedVoteInsightRequest request = dataCollector.collect(
                    vote.getId(), maleUser20s.getId(), optionA.getId());

            // then
            assertThat(request.voteTitle()).isEqualTo("짜장면 vs 짬뽕");
            assertThat(request.optionALabel()).isEqualTo("짜장면");
            assertThat(request.optionACount()).isEqualTo(2);
            assertThat(request.optionARatio()).isEqualTo(67); // 2/3 ≈ 67%
            assertThat(request.optionBLabel()).isEqualTo("짬뽕");
            assertThat(request.optionBCount()).isEqualTo(1);
            assertThat(request.totalParticipants()).isEqualTo(3);
        }

        @Test
        @DisplayName("사용자 프로필 정보를 수집한다")
        void collectsUserProfile() {
            // given
            participate(maleUser20s, optionA);

            // when
            PersonalizedVoteInsightRequest request = dataCollector.collect(
                    vote.getId(), maleUser20s.getId(), optionA.getId());

            // then
            assertThat(request.userGender()).isEqualTo("MALE");
            assertThat(request.userAgeGroup()).isEqualTo("20s");
            assertThat(request.userSelectedOption()).isEqualTo("짜장면");
        }

        @Test
        @DisplayName("같은 성별 비율을 계산한다")
        void calculatesSameGenderRatio() {
            // given: 남성 2명 중 1명이 짜장면, 1명이 짬뽕 선택
            participate(maleUser20s, optionA);
            participate(maleUser30s, optionB);
            participate(femaleUser30s, optionA);

            // when - maleUser20s 관점에서 조회
            PersonalizedVoteInsightRequest request = dataCollector.collect(
                    vote.getId(), maleUser20s.getId(), optionA.getId());

            // then: 남성 2명 중 1명이 짜장면 선택 → 50%
            assertThat(request.sameGenderRatio()).isEqualTo(50);
        }

        @Test
        @DisplayName("같은 연령대 비율을 계산한다")
        void calculatesSameAgeGroupRatio() {
            // given: 30대 2명 중 1명이 짜장면, 1명이 짬뽕
            participate(maleUser20s, optionA);
            participate(femaleUser30s, optionA);
            participate(maleUser30s, optionB);

            // when - femaleUser30s 관점에서 조회
            PersonalizedVoteInsightRequest request = dataCollector.collect(
                    vote.getId(), femaleUser30s.getId(), optionA.getId());

            // then: 30대 2명 중 1명이 짜장면 선택 → 50%
            assertThat(request.sameAgeGroupRatio()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("VoteResultQueryService 통합")
    class VoteResultQueryTest {

        @Test
        @DisplayName("투표 참여자는 개인화된 AI 인사이트를 받는다")
        void participantGetsPersonalizedAiInsight() {
            // given
            participate(maleUser20s, optionA);
            participate(femaleUser30s, optionB);

            AiInsightResult mockResult = new AiInsightResult(
                    "당신은 다수파입니다!",
                    "남성 20대의 100%가 짜장면을 선택했어요."
            );
            given(aiInsightUseCase.generatePersonalizedInsight(any()))
                    .willReturn(Optional.of(mockResult));

            // when
            VoteResultDetail result = voteResultQueryUseCase.getResult(
                    vote.getId(), maleUser20s.getId());

            // then
            assertThat(result.voted()).isTrue();
            assertThat(result.mySelectedOptionId()).isEqualTo(optionA.getId());
            assertThat(result.aiInsight().available()).isTrue();
            assertThat(result.aiInsight().headline()).isEqualTo("당신은 다수파입니다!");
            assertThat(result.aiInsight().body()).contains("남성 20대");
        }

        @Test
        @DisplayName("투표 미참여자는 AI 인사이트를 받지 않는다")
        void nonParticipantDoesNotGetAiInsight() {
            // given
            participate(femaleUser30s, optionA);
            // maleUser20s는 참여하지 않음

            // when
            VoteResultDetail result = voteResultQueryUseCase.getResult(
                    vote.getId(), maleUser20s.getId());

            // then
            assertThat(result.voted()).isFalse();
            assertThat(result.aiInsight().available()).isFalse();

            // AI 호출이 없어야 함
            verify(aiInsightUseCase, never()).generatePersonalizedInsight(any());
        }

        @Test
        @DisplayName("다른 성별/연령대 사용자는 다른 인사이트를 받는다")
        void differentDemographicsGetDifferentInsights() {
            // given
            participate(maleUser20s, optionA);
            participate(femaleUser30s, optionA);

            AiInsightResult maleInsight = new AiInsightResult("남성 인사이트", "남성용 바디");
            AiInsightResult femaleInsight = new AiInsightResult("여성 인사이트", "여성용 바디");

            given(aiInsightUseCase.generatePersonalizedInsight(any()))
                    .willReturn(Optional.of(maleInsight))
                    .willReturn(Optional.of(femaleInsight));

            // when
            VoteResultDetail maleResult = voteResultQueryUseCase.getResult(
                    vote.getId(), maleUser20s.getId());
            VoteResultDetail femaleResult = voteResultQueryUseCase.getResult(
                    vote.getId(), femaleUser30s.getId());

            // then
            assertThat(maleResult.aiInsight().headline()).isEqualTo("남성 인사이트");
            assertThat(femaleResult.aiInsight().headline()).isEqualTo("여성 인사이트");

            // AI가 2번 호출됨 (다른 캐시 키)
            verify(aiInsightUseCase, times(2)).generatePersonalizedInsight(any());
        }

        @Test
        @DisplayName("AI 호출 실패 시 인사이트 unavailable 반환")
        void aiFailureReturnsUnavailable() {
            // given
            participate(maleUser20s, optionA);

            given(aiInsightUseCase.generatePersonalizedInsight(any()))
                    .willReturn(Optional.empty());

            // when
            VoteResultDetail result = voteResultQueryUseCase.getResult(
                    vote.getId(), maleUser20s.getId());

            // then
            assertThat(result.aiInsight().available()).isFalse();
        }

        @Test
        @DisplayName("비회원은 AI 인사이트를 받지 않는다")
        void guestDoesNotGetAiInsight() {
            // given
            participate(maleUser20s, optionA);

            // when
            VoteResultDetail result = voteResultQueryUseCase.getResult(
                    vote.getId(), null);

            // then
            assertThat(result.insight().locked()).isTrue();
            assertThat(result.aiInsight().available()).isFalse();

            verify(aiInsightUseCase, never()).generatePersonalizedInsight(any());
        }
    }

    @Nested
    @DisplayName("캐시 동작 검증")
    class CacheTest {

        @Test
        @DisplayName("동일 조건 재조회 시 캐시 히트로 AI 재호출 없음")
        void cacheHitOnSameCondition() {
            // given
            participate(maleUser20s, optionA);

            AiInsightResult mockResult = new AiInsightResult("캐시됨", "캐시된 바디");
            given(aiInsightUseCase.generatePersonalizedInsight(any()))
                    .willReturn(Optional.of(mockResult));

            // when - 첫 번째 조회
            VoteResultDetail first = voteResultQueryUseCase.getResult(
                    vote.getId(), maleUser20s.getId());

            // when - 두 번째 조회 (동일 조건)
            VoteResultDetail second = voteResultQueryUseCase.getResult(
                    vote.getId(), maleUser20s.getId());

            // then
            assertThat(first.aiInsight().headline()).isEqualTo("캐시됨");
            assertThat(second.aiInsight().headline()).isEqualTo("캐시됨");

            // AI는 1번만 호출됨 (두 번째는 캐시 히트)
            verify(aiInsightUseCase, times(1)).generatePersonalizedInsight(any());
        }
    }
}
