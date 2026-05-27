package com.ject.vs.recommendation.port;

import com.ject.vs.config.AdminProperties;
import com.ject.vs.vote.domain.RecommendedVoteRepository;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationCommandServiceTest {

    @InjectMocks
    private RecommendationCommandService service;

    @Mock
    private AdminProperties adminProperties;

    @Mock
    private RecommendedVoteRepository recommendedVoteRepository;

    @Mock
    private VoteRepository voteRepository;

    private final Long ADMIN_USER_ID = 1L;
    private final Long NORMAL_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        given(adminProperties.userIds()).willReturn(List.of(ADMIN_USER_ID));
    }

    @Nested
    @DisplayName("관리자 권한 검증")
    class AdminValidation {

        @Test
        @DisplayName("관리자 사용자는 추천을 설정할 수 있다")
        void admin_can_set_recommendations() {
            List<Long> voteIds = List.of(10L, 20L);
            given(voteRepository.findAllById(voteIds))
                    .willReturn(List.of(mock(Vote.class), mock(Vote.class)));

            service.setTodayRecommendations(ADMIN_USER_ID, voteIds);

            verify(recommendedVoteRepository).deleteAllByRecommendedDate(any());
            verify(recommendedVoteRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("일반 사용자는 추천 설정이 불가능하다")
        void non_admin_cannot_set_recommendations() {
            assertThatThrownBy(() ->
                    service.setTodayRecommendations(NORMAL_USER_ID, List.of(10L))
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한이 없습니다");
        }
    }

    @Nested
    @DisplayName("입력값 검증")
    class InputValidation {

        @Test
        @DisplayName("voteIds가 null이거나 비어있으면 예외가 발생한다")
        void empty_voteIds_throws_exception() {
            assertThatThrownBy(() ->
                    service.setTodayRecommendations(ADMIN_USER_ID, List.of())
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("하나 이상 지정");

            assertThatThrownBy(() ->
                    service.setTodayRecommendations(ADMIN_USER_ID, null)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("중복된 voteId가 있으면 예외가 발생한다")
        void duplicate_voteIds_throws_exception() {
            assertThatThrownBy(() ->
                    service.setTodayRecommendations(ADMIN_USER_ID, List.of(10L, 10L, 20L))
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("중복");
        }

        @Test
        @DisplayName("존재하지 않는 voteId가 있으면 예외가 발생한다")
        void non_existing_vote_throws_exception() {
            given(voteRepository.findAllById(List.of(10L, 99L)))
                    .willReturn(List.of(mock(Vote.class))); // 99L은 없음

            assertThatThrownBy(() ->
                    service.setTodayRecommendations(ADMIN_USER_ID, List.of(10L, 99L))
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는");
        }
    }

    @Nested
    @DisplayName("추천 저장 로직")
    class SaveLogic {

        @Test
        @DisplayName("기존 추천을 삭제하고 새로운 추천을 저장한다")
        void replaces_existing_recommendations() {
            List<Long> voteIds = List.of(100L, 200L, 300L);
            given(voteRepository.findAllById(voteIds))
                    .willReturn(List.of(mock(Vote.class), mock(Vote.class), mock(Vote.class)));

            service.setTodayRecommendations(ADMIN_USER_ID, voteIds);

            verify(recommendedVoteRepository).deleteAllByRecommendedDate(any());
            verify(recommendedVoteRepository).saveAll(argThat(list -> {
                List<com.ject.vs.vote.domain.RecommendedVote> recommendations = (List<com.ject.vs.vote.domain.RecommendedVote>) list;
                return recommendations.size() == 3
                        && recommendations.get(0).getDisplayOrder() == 1
                        && recommendations.get(1).getDisplayOrder() == 2
                        && recommendations.get(2).getDisplayOrder() == 3;
            }));
        }
    }
}