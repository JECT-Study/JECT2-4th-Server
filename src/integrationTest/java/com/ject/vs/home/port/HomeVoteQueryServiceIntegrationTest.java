package com.ject.vs.home.port;

import com.ject.vs.home.port.in.HomeVoteQueryUseCase;
import com.ject.vs.home.port.in.HomeVoteQueryUseCase.VoteListResult;
import com.ject.vs.home.port.in.HomeVoteQueryUseCase.VoteSortType;
import com.ject.vs.support.VoteIntegrationTestSupport;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HomeVoteQueryService 통합 테스트 (excludeEnded 필터)")
class HomeVoteQueryServiceIntegrationTest extends VoteIntegrationTestSupport {

    @Autowired
    private HomeVoteQueryUseCase homeVoteQueryUseCase;

    @Nested
    @DisplayName("excludeEnded 필터 동작")
    class ExcludeEndedFilter {

        @Test
        @DisplayName("excludeEnded=false이면 종료된 투표도 함께 반환한다")
        void returnsEndedVotesWhenExcludeEndedIsFalse() {
            // given
            createOngoingVote("진행중-1");
            createOngoingVote("진행중-2");
            createEndedVote("종료됨-1");
            createEndedVote("종료됨-2");

            // when
            VoteListResult result = homeVoteQueryUseCase.getVoteList(null, 10, VoteSortType.LATEST, false);

            // then
            assertThat(result.items()).hasSize(4);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("excludeEnded=true이면 진행 중인 투표만 반환한다")
        void returnsOnlyOngoingWhenExcludeEndedIsTrue() {
            // given
            createOngoingVote("진행중-A");
            createOngoingVote("진행중-B");
            createEndedVote("종료됨-X");
            createEndedVote("종료됨-Y");

            // when
            VoteListResult result = homeVoteQueryUseCase.getVoteList(null, 10, VoteSortType.LATEST, true);

            // then
            assertThat(result.items()).hasSize(2);
            assertThat(result.items())
                    .extracting(HomeVoteQueryUseCase.VoteListItem::title)
                    .containsExactly("진행중-B", "진행중-A"); // id desc
            assertThat(result.items())
                    .extracting(HomeVoteQueryUseCase.VoteListItem::status)
                    .containsOnly(VoteStatus.ONGOING);
        }
    }

    @Nested
    @DisplayName("정렬 + 필터 조합")
    class SortAndFilter {

        @Test
        @DisplayName("LATEST + excludeEnded=true → id 내림차순으로 진행중만")
        void latestWithExcludeEnded() {
            // given
            // id가 증가하는 순서로 생성 → 마지막에 만든 것이 가장 최신 (id desc 기준 첫 번째)
            createOngoingVote("최신-1");
            createOngoingVote("최신-2");
            createOngoingVote("최신-3");
            createEndedVote("종료-무시");

            // when
            VoteListResult result = homeVoteQueryUseCase.getVoteList(null, 10, VoteSortType.LATEST, true);

            // then
            assertThat(result.items()).hasSize(3);
            assertThat(result.items().get(0).title()).isEqualTo("최신-3"); // 가장 높은 id
            assertThat(result.items().get(2).title()).isEqualTo("최신-1"); // 가장 낮은 id
        }

        @Test
        @DisplayName("POPULAR + excludeEnded=true → 조회수 내림차순으로 진행중만")
        void popularWithExcludeEnded() {
            // given
            Vote low = createOngoingVote("인기-낮음");
            Vote high = createOngoingVote("인기-높음");
            createEndedVote("종료-무시");

            saveViewCount(low.getId(), 5L);
            saveViewCount(high.getId(), 120L);

            // when
            VoteListResult result = homeVoteQueryUseCase.getVoteList(null, 10, VoteSortType.POPULAR, true);

            // then
            assertThat(result.items()).hasSize(2);
            assertThat(result.items().get(0).title()).isEqualTo("인기-높음");
            assertThat(result.items().get(1).title()).isEqualTo("인기-낮음");
        }

        @Test
        @DisplayName("ENDING_SOON은 excludeEnded 값과 무관하게 항상 진행중 + 종료임박순")
        void endingSoonAlwaysFiltersEnded() {
            // given
            createOngoingVoteWithEndAt("조금후", FIXED_NOW.plus(Duration.ofHours(5)));
            createOngoingVoteWithEndAt("곧종료", FIXED_NOW.plus(Duration.ofHours(1)));
            createEndedVote("이미종료");

            // when
            VoteListResult resultFalse = homeVoteQueryUseCase.getVoteList(null, 10, VoteSortType.ENDING_SOON, false);
            VoteListResult resultTrue = homeVoteQueryUseCase.getVoteList(null, 10, VoteSortType.ENDING_SOON, true);

            // then
            assertThat(resultFalse.items()).hasSize(2);
            assertThat(resultTrue.items()).hasSize(2);

            // 종료 임박순 (endAt ASC)
            assertThat(resultTrue.items().get(0).title()).isEqualTo("곧종료");
            assertThat(resultTrue.items().get(1).title()).isEqualTo("조금후");
        }
    }

    @Nested
    @DisplayName("커서 기반 페이지네이션 + 필터")
    class Pagination {

        @Test
        @DisplayName("excludeEnded=true + size 제한 시 hasNext와 nextCursor가 정상 동작한다")
        void paginationWithExcludeEndedTrue() {
            // given
            createOngoingVote("p-1");
            createOngoingVote("p-2");
            createOngoingVote("p-3");
            createOngoingVote("p-4");
            createEndedVote("종료-무시");

            // when
            // 첫 페이지
            VoteListResult first = homeVoteQueryUseCase.getVoteList(null, 2, VoteSortType.LATEST, true);
            assertThat(first.items()).hasSize(2);
            assertThat(first.hasNext()).isTrue();
            assertThat(first.nextCursor()).isNotNull();

            // 두 번째 페이지
            VoteListResult second = homeVoteQueryUseCase.getVoteList(first.nextCursor(), 2, VoteSortType.LATEST, true);

            // then
            assertThat(second.items()).hasSize(2);
            assertThat(second.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("커서 기반 페이지네이션 (Keyset Pagination)")
    class CursorPagination {

        @Test
        @DisplayName("LATEST - 문자열 커서로도 정상 페이지네이션 동작")
        void latestWithStringCursor() {
            // given
            createOngoingVote("a");
            createOngoingVote("b");
            createOngoingVote("c");

            // when
            VoteListResult first = homeVoteQueryUseCase.getVoteList(null, 2, VoteSortType.LATEST, true);

            // then
            assertThat(first.items()).hasSize(2);
            assertThat(first.hasNext()).isTrue();
            assertThat(first.nextCursor()).isNotNull();

            VoteListResult second = homeVoteQueryUseCase.getVoteList(first.nextCursor(), 2, VoteSortType.LATEST, true);
            assertThat(second.items()).hasSize(1);
            assertThat(second.hasNext()).isFalse();
            assertThat(second.nextCursor()).isNull();
        }

        @Test
        @DisplayName("ENDING_SOON - 복합 커서(endAt:id)로 정확한 Keyset 페이지네이션 동작 (중복/누락 없음)")
        void endingSoonWithCompositeCursor() {
            // given: 종료 시간 기준 id2(+1h) < id3(+2h) < id1(+3h)
            createOngoingVoteWithEndAt("vote-1", FIXED_NOW.plus(Duration.ofHours(3)));
            createOngoingVoteWithEndAt("vote-2", FIXED_NOW.plus(Duration.ofHours(1)));
            createOngoingVoteWithEndAt("vote-3", FIXED_NOW.plus(Duration.ofHours(2)));

            // when: size=2로 첫 페이지
            VoteListResult first = homeVoteQueryUseCase.getVoteList(null, 2, VoteSortType.ENDING_SOON, true);

            // then
            assertThat(first.items()).hasSize(2);
            assertThat(first.items().get(0).title()).isEqualTo("vote-2");
            assertThat(first.items().get(1).title()).isEqualTo("vote-3");
            assertThat(first.hasNext()).isTrue();
            assertThat(first.nextCursor()).isNotNull();
            assertThat(first.nextCursor()).contains(":");

            // when: 두 번째 페이지
            VoteListResult second = homeVoteQueryUseCase.getVoteList(first.nextCursor(), 2, VoteSortType.ENDING_SOON, true);

            // then: 중복 없이 마지막 항목만 반환
            assertThat(second.items()).hasSize(1);
            assertThat(second.items().get(0).title()).isEqualTo("vote-1");
            assertThat(second.hasNext()).isFalse();
            assertThat(second.nextCursor()).isNull();
        }

        @Test
        @DisplayName("ENDING_SOON - 원본 버그 재현 케이스 (id1+3h, id2+1h, id3+2h, size=2)")
        void endingSoonBugReproductionCase() {
            // given (피드백에 나온 정확한 예시)
            createOngoingVoteWithEndAt("vote-1", FIXED_NOW.plus(Duration.ofHours(3)));
            createOngoingVoteWithEndAt("vote-2", FIXED_NOW.plus(Duration.ofHours(1)));
            createOngoingVoteWithEndAt("vote-3", FIXED_NOW.plus(Duration.ofHours(2)));

            // 1페이지
            VoteListResult page1 = homeVoteQueryUseCase.getVoteList(null, 2, VoteSortType.ENDING_SOON, true);
            assertThat(page1.items()).extracting(HomeVoteQueryUseCase.VoteListItem::title).containsExactly("vote-2", "vote-3");

            // 2페이지 (이전 버그에서는 id=2가 중복됨)
            VoteListResult page2 = homeVoteQueryUseCase.getVoteList(page1.nextCursor(), 2, VoteSortType.ENDING_SOON, true);

            assertThat(page2.items()).hasSize(1);
            assertThat(page2.items().get(0).title()).isEqualTo("vote-1");
            // id=2가 절대 다시 나오면 안 됨
            assertThat(page2.items()).extracting(HomeVoteQueryUseCase.VoteListItem::title).doesNotContain("vote-2");
        }

        @Test
        @DisplayName("excludeEnded + ENDING_SOON 복합 커서 조합")
        void endingSoonWithExcludeEnded() {
            createOngoingVoteWithEndAt("soon", FIXED_NOW.plus(Duration.ofHours(1)));
            createOngoingVoteWithEndAt("later", FIXED_NOW.plus(Duration.ofHours(5)));
            createEndedVote("already-ended");

            VoteListResult result = homeVoteQueryUseCase.getVoteList(null, 10, VoteSortType.ENDING_SOON, true);

            assertThat(result.items()).hasSize(2);
            assertThat(result.items().get(0).title()).isEqualTo("soon");
        }

        @Test
        @DisplayName("잘못된 형식의 cursor가 들어와도 첫 페이지로 안전하게 동작")
        void malformedCursorFallsBackToFirstPage() {
            createOngoingVote("v1");
            createOngoingVote("v2");

            VoteListResult result = homeVoteQueryUseCase.getVoteList("invalid-cursor-format", 10, VoteSortType.ENDING_SOON, true);

            assertThat(result.items()).hasSize(2);
            assertThat(result.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("진행 중 투표가 없을 때 excludeEnded=true이면 빈 결과 반환")
        void returnsEmptyWhenNoOngoingAndExcludeEndedTrue() {
            // given
            createEndedVote("종료-1");
            createEndedVote("종료-2");

            // when
            VoteListResult result = homeVoteQueryUseCase.getVoteList(null, 10, VoteSortType.LATEST, true);

            // then
            assertThat(result.items()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }
    }

}
