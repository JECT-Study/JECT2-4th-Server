package com.ject.vs.home.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.home.adapter.web.dto.HomeVoteListResponse;
import com.ject.vs.support.VoteIntegrationTestSupport;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("GET /api/home/votes 통합 테스트 (excludeEnded 필터)")
class HomeControllerIntegrationTest extends VoteIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("excludeEnded 필터 동작")
    class ExcludeEndedFilter {

        @Test
        @DisplayName("excludeEnded=false (기본값)일 때 종료된 투표도 포함해서 반환한다")
        void whenExcludeEndedIsFalse_returnsBothOngoingAndEnded() throws Exception {
            // given: 진행중 2개 + 종료 2개
            createOngoingVote("진행중-1");
            createOngoingVote("진행중-2");
            createEndedVote("종료됨-1");
            createEndedVote("종료됨-2");

            // when
            HomeVoteListResponse response = callApi(null, 10, "LATEST", false);

            // then
            assertThat(response.votes()).hasSize(4);

            // excludeEnded=false이면 종료된 것도 포함되어야 함
            assertThat(response.votes())
                    .extracting(HomeVoteListResponse.VoteListItem::title)
                    .containsExactlyInAnyOrder("진행중-1", "진행중-2", "종료됨-1", "종료됨-2");
        }

        @Test
        @DisplayName("excludeEnded=true일 때 진행 중인 투표만 반환한다")
        void whenExcludeEndedIsTrue_returnsOnlyOngoing() throws Exception {
            // given
            createOngoingVote("진행중-A");
            createOngoingVote("진행중-B");
            createEndedVote("종료됨-X");
            createEndedVote("종료됨-Y");

            // when
            HomeVoteListResponse response = callApi(null, 10, "LATEST", true);

            // then
            assertThat(response.votes()).hasSize(2);
            assertThat(response.votes())
                    .extracting(HomeVoteListResponse.VoteListItem::title)
                    .containsExactly("진행중-B", "진행중-A");
            assertThat(response.votes())
                    .extracting(HomeVoteListResponse.VoteListItem::status)
                    .containsOnly(VoteStatus.ONGOING);
        }

        @Test
        @DisplayName("종료된 투표만 있을 때 excludeEnded=true이면 빈 목록을 반환한다")
        void whenOnlyEndedVotesExist_andExcludeEndedTrue_returnsEmpty() throws Exception {
            // given
            createEndedVote("종료-1");
            createEndedVote("종료-2");

            // when
            HomeVoteListResponse response = callApi(null, 10, "LATEST", true);

            // then
            assertThat(response.votes()).isEmpty();
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("정렬별 excludeEnded 동작")
    class SortWithFilter {

        @Test
        @DisplayName("POPULAR 정렬 + excludeEnded=true → 진행중 투표만 조회수 순으로 반환")
        void popularSortWithExcludeEnded() throws Exception {
            // given
            Vote v1 = createOngoingVote("인기-낮음");
            Vote v2 = createOngoingVote("인기-높음");
            createEndedVote("종료-무시");

            // 조회수 설정 (v2가 더 인기)
            saveViewCount(v1.getId(), 10L);
            saveViewCount(v2.getId(), 100L);

            // when
            HomeVoteListResponse response = callApi(null, 10, "POPULAR", true);

            // then
            assertThat(response.votes()).hasSize(2);
            assertThat(response.votes().get(0).title()).isEqualTo("인기-높음");
            assertThat(response.votes().get(1).title()).isEqualTo("인기-낮음");
        }

        @Test
        @DisplayName("ENDING_SOON 정렬은 excludeEnded 값과 무관하게 항상 진행중만 반환")
        void endingSoonAlwaysFiltersEnded() throws Exception {
            // given
            // endAt이 더 빠른 것(먼저 종료)이 앞으로 와야 함
            Vote soon = createOngoingVoteWithCustomEndAt("종료임박", FIXED_NOW.plus(Duration.ofHours(1)));
            Vote later = createOngoingVoteWithCustomEndAt("조금더여유", FIXED_NOW.plus(Duration.ofHours(5)));
            createEndedVote("이미종료");

            // when
            HomeVoteListResponse responseFalse = callApi(null, 10, "ENDING_SOON", false);
            HomeVoteListResponse responseTrue = callApi(null, 10, "ENDING_SOON", true);

            // then
            assertThat(responseFalse.votes()).hasSize(2);
            assertThat(responseTrue.votes()).hasSize(2);

            // 종료 임박순: endAt ASC
            assertThat(responseTrue.votes().get(0).title()).isEqualTo("종료임박");
            assertThat(responseTrue.votes().get(1).title()).isEqualTo("조금더여유");
        }
    }

    @Nested
    @DisplayName("커서 기반 페이지네이션 + 필터")
    class PaginationWithFilter {

        @Test
        @DisplayName("excludeEnded=true + size=1 → hasNext=true, nextCursor 존재")
        void paginationWithExcludeEnded() throws Exception {
            // given
            createOngoingVote("1");
            createOngoingVote("2");
            createOngoingVote("3");
            createEndedVote("종료-무시");

            // when
            // 첫 페이지
            HomeVoteListResponse first = callApi(null, 1, "LATEST", true);
            assertThat(first.votes()).hasSize(1);
            assertThat(first.hasNext()).isTrue();
            assertThat(first.nextCursor()).isNotNull();

            // 두 번째 페이지
            HomeVoteListResponse second = callApi(first.nextCursor(), 1, "LATEST", true);

            // 세 번째 페이지
            HomeVoteListResponse third = callApi(second.nextCursor(), 1, "LATEST", true);

            // then
            assertThat(third.votes()).hasSize(1);
            assertThat(third.hasNext()).isFalse();
            assertThat(third.nextCursor()).isNull();
        }
    }

    private HomeVoteListResponse callApi(Long cursor, int size, String sort, boolean excludeEnded) throws Exception {
        var builder = get("/api/home/votes")
                .param("size", String.valueOf(size))
                .param("sort", sort)
                .param("excludeEnded", String.valueOf(excludeEnded))
                .contentType(MediaType.APPLICATION_JSON);

        if (cursor != null) {
            builder.param("cursor", String.valueOf(cursor));
        }

        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readValue(json, HomeVoteListResponse.class);
    }
}
