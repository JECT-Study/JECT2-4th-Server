package com.ject.vs.ai.port;

import com.ject.vs.ai.port.in.AiInsightUseCase.PersonalizedVoteInsightRequest;
import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.vote.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonalizedInsightDataCollectorTest {

    @Mock VoteRepository voteRepository;
    @Mock VoteOptionRepository voteOptionRepository;
    @Mock VoteParticipationRepository voteParticipationRepository;
    @Mock UserRepository userRepository;

    PersonalizedInsightDataCollector collector;

    static final Clock CLOCK = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);

    Vote vote;
    VoteOption optionA;
    VoteOption optionB;
    User maleUser;
    User femaleUser;

    @BeforeEach
    void setUp() {
        collector = new PersonalizedInsightDataCollector(
                voteRepository, voteOptionRepository, voteParticipationRepository, userRepository, CLOCK);

        Clock pastClock = Clock.fixed(Instant.parse("2025-05-30T00:00:00Z"), ZoneOffset.UTC);
        vote = Vote.create("짜장면 vs 짬뽕", null, "thumb.png", null, Duration.ofHours(24), pastClock);

        optionA = createOption(vote, "짜장면", 0, 10L);
        optionB = createOption(vote, "짬뽕", 1, 20L);

        maleUser = createUser(1L, Gender.MALE, Year.of(2000));
        femaleUser = createUser(2L, Gender.FEMALE, Year.of(1995));
    }

    VoteOption createOption(Vote vote, String label, int position, Long id) {
        VoteOption option = VoteOption.of(vote, label, position);
        try {
            var idField = option.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(option, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return option;
    }

    User createUser(Long id, Gender gender, Year birthYear) {
        User user = User.createWithEmail("test" + id + "@test.com");
        try {
            var idField = user.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);

            var genderField = user.getClass().getDeclaredField("gender");
            genderField.setAccessible(true);
            genderField.set(user, gender);

            var birthField = user.getClass().getDeclaredField("birthYear");
            birthField.setAccessible(true);
            birthField.set(user, birthYear);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    @Nested
    @DisplayName("collect")
    class Collect {

        @Test
        @DisplayName("기본 투표 통계 수집")
        void collectsBasicVoteStatistics() {
            // given
            Long voteId = 1L, userId = 1L, selectedOptionId = 10L;

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(voteId)).willReturn(List.of(optionA, optionB));
            given(voteParticipationRepository.countByVoteId(voteId)).willReturn(100L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(voteId, 10L)).willReturn(60L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(voteId, 20L)).willReturn(40L);
            given(voteParticipationRepository.findGenderDistributionByVote(voteId))
                    .willReturn(List.of(new GenderCount(Gender.MALE, 50L), new GenderCount(Gender.FEMALE, 50L)));
            given(voteParticipationRepository.findAllUserIdsByVoteId(voteId)).willReturn(List.of(1L, 2L));
            given(userRepository.findAllById(anyList())).willReturn(List.of(maleUser, femaleUser));
            given(userRepository.findById(userId)).willReturn(Optional.of(maleUser));
            given(voteParticipationRepository.countByVoteIdAndOptionIdAndGender(voteId, selectedOptionId, Gender.MALE))
                    .willReturn(40L);
            given(voteParticipationRepository.countByVoteIdAndGender(voteId, Gender.MALE)).willReturn(50L);
            given(voteParticipationRepository.findOptionCountsByVoteIdAndGender(voteId, Gender.MALE))
                    .willReturn(List.<Object[]>of(new Object[]{10L, 40L}));
            given(voteParticipationRepository.findUserIdsByVoteIdAndOptionId(voteId, selectedOptionId))
                    .willReturn(List.of(1L));
            given(voteParticipationRepository.findByVoteIdAndUserId(voteId, 1L))
                    .willReturn(Optional.of(VoteParticipation.ofMember(voteId, 1L, selectedOptionId)));

            // when
            PersonalizedVoteInsightRequest request = collector.collect(voteId, userId, selectedOptionId);

            // then
            assertThat(request.voteTitle()).isEqualTo("짜장면 vs 짬뽕");
            assertThat(request.optionALabel()).isEqualTo("짜장면");
            assertThat(request.optionACount()).isEqualTo(60);
            assertThat(request.optionARatio()).isEqualTo(60);
            assertThat(request.optionBLabel()).isEqualTo("짬뽕");
            assertThat(request.optionBCount()).isEqualTo(40);
            assertThat(request.totalParticipants()).isEqualTo(100);
        }

        @Test
        @DisplayName("사용자 프로필 정보 수집")
        void collectsUserProfile() {
            // given
            Long voteId = 1L, userId = 1L, selectedOptionId = 10L;

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(voteId)).willReturn(List.of(optionA, optionB));
            given(voteParticipationRepository.countByVoteId(voteId)).willReturn(100L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(voteId, 10L)).willReturn(60L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(voteId, 20L)).willReturn(40L);
            given(voteParticipationRepository.findGenderDistributionByVote(voteId)).willReturn(List.of());
            given(voteParticipationRepository.findAllUserIdsByVoteId(voteId)).willReturn(List.of());
            given(userRepository.findAllById(anyList())).willReturn(List.of());
            given(userRepository.findById(userId)).willReturn(Optional.of(maleUser));
            given(voteParticipationRepository.countByVoteIdAndOptionIdAndGender(voteId, selectedOptionId, Gender.MALE))
                    .willReturn(30L);
            given(voteParticipationRepository.countByVoteIdAndGender(voteId, Gender.MALE)).willReturn(50L);
            given(voteParticipationRepository.findOptionCountsByVoteIdAndGender(voteId, Gender.MALE))
                    .willReturn(List.<Object[]>of(new Object[]{10L, 30L}));
            given(voteParticipationRepository.findUserIdsByVoteIdAndOptionId(voteId, selectedOptionId))
                    .willReturn(List.of(1L));
            given(voteParticipationRepository.findByVoteIdAndUserId(voteId, 1L))
                    .willReturn(Optional.of(VoteParticipation.ofMember(voteId, 1L, selectedOptionId)));

            // when
            PersonalizedVoteInsightRequest request = collector.collect(voteId, userId, selectedOptionId);

            // then
            assertThat(request.userGender()).isEqualTo("MALE");
            assertThat(request.userAgeGroup()).isEqualTo("20s"); // 2000년생 → 25세 → 20대
            assertThat(request.userSelectedOption()).isEqualTo("짜장면");
        }

        @Test
        @DisplayName("같은 성별 비율 계산")
        void calculatesSameGenderRatio() {
            // given
            Long voteId = 1L, userId = 1L, selectedOptionId = 10L;

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(voteId)).willReturn(List.of(optionA, optionB));
            given(voteParticipationRepository.countByVoteId(voteId)).willReturn(100L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(voteId, 10L)).willReturn(60L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(voteId, 20L)).willReturn(40L);
            given(voteParticipationRepository.findGenderDistributionByVote(voteId)).willReturn(List.of());
            given(voteParticipationRepository.findAllUserIdsByVoteId(voteId)).willReturn(List.of());
            given(userRepository.findAllById(anyList())).willReturn(List.of());
            given(userRepository.findById(userId)).willReturn(Optional.of(maleUser));
            // 남성 50명 중 35명이 같은 옵션 선택 → 70%
            given(voteParticipationRepository.countByVoteIdAndOptionIdAndGender(voteId, selectedOptionId, Gender.MALE))
                    .willReturn(35L);
            given(voteParticipationRepository.countByVoteIdAndGender(voteId, Gender.MALE)).willReturn(50L);
            given(voteParticipationRepository.findOptionCountsByVoteIdAndGender(voteId, Gender.MALE))
                    .willReturn(List.<Object[]>of(new Object[]{10L, 35L}));
            given(voteParticipationRepository.findUserIdsByVoteIdAndOptionId(voteId, selectedOptionId))
                    .willReturn(List.of(1L));
            given(voteParticipationRepository.findByVoteIdAndUserId(voteId, 1L))
                    .willReturn(Optional.of(VoteParticipation.ofMember(voteId, 1L, selectedOptionId)));

            // when
            PersonalizedVoteInsightRequest request = collector.collect(voteId, userId, selectedOptionId);

            // then
            assertThat(request.sameGenderRatio()).isEqualTo(70);
            assertThat(request.sameGenderMajorityOption()).isEqualTo("짜장면");
        }

        @Test
        @DisplayName("사용자 정보 없으면 null 반환")
        void returnsNullForMissingUserInfo() {
            // given
            Long voteId = 1L, userId = 999L, selectedOptionId = 10L;

            given(voteRepository.findById(voteId)).willReturn(Optional.of(vote));
            given(voteOptionRepository.findByVoteIdOrderByPosition(voteId)).willReturn(List.of(optionA, optionB));
            given(voteParticipationRepository.countByVoteId(voteId)).willReturn(100L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(voteId, 10L)).willReturn(60L);
            given(voteParticipationRepository.countByVoteIdAndOptionId(voteId, 20L)).willReturn(40L);
            given(voteParticipationRepository.findGenderDistributionByVote(voteId)).willReturn(List.of());
            given(voteParticipationRepository.findAllUserIdsByVoteId(voteId)).willReturn(List.of());
            given(userRepository.findAllById(anyList())).willReturn(List.of());
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when
            PersonalizedVoteInsightRequest request = collector.collect(voteId, userId, selectedOptionId);

            // then
            assertThat(request.userGender()).isNull();
            assertThat(request.userAgeGroup()).isNull();
            assertThat(request.sameGenderRatio()).isEqualTo(0);
            assertThat(request.sameAgeGroupRatio()).isEqualTo(0);
        }
    }
}
