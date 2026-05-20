package com.ject.vs.home.adapter.web;

import com.ject.vs.home.adapter.web.dto.HomeHotTopicResponse;
import com.ject.vs.home.adapter.web.dto.HomeRecommendationResponse;
import com.ject.vs.home.adapter.web.dto.HomeVoteListResponse;
import com.ject.vs.home.port.in.HomeVoteQueryUseCase;
import com.ject.vs.home.port.in.HomeVoteQueryUseCase.VoteSortType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "홈", description = "홈 화면 관련 API")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeVoteQueryUseCase homeVoteQueryUseCase;

    @Operation(summary = "오늘의 추천 조회", description = "운영진이 선정한 오늘의 추천 투표 목록을 조회합니다.")
    @GetMapping("/recommendations")
    public HomeRecommendationResponse getRecommendations() {
        return HomeRecommendationResponse.from(homeVoteQueryUseCase.getRecommendations());
    }

    @Operation(summary = "핫토픽 TOP 3 조회", description = "인기 점수 기준 상위 3개 투표를 조회합니다. 인기 점수 = (참여 수 × 0.7) + (조회 수 × 0.3)")
    @GetMapping("/hot-topics")
    public HomeHotTopicResponse getHotTopics() {
        return HomeHotTopicResponse.from(homeVoteQueryUseCase.getHotTopics());
    }

    @Operation(summary = "전체 투표 목록 조회", description = "전체 투표 목록을 조회합니다. 커서 기반 페이지네이션을 지원합니다.")
    @GetMapping("/votes")
    public HomeVoteListResponse getVoteList(
            @Parameter(description = "다음 페이지 조회를 위한 커서 (이전 응답의 nextCursor)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준: LATEST(최신순), POPULAR(인기순), ENDING_SOON(종료임박순)")
            @RequestParam(defaultValue = "LATEST") VoteSortType sort
    ) {
        return HomeVoteListResponse.from(homeVoteQueryUseCase.getVoteList(cursor, size, sort));
    }
}
