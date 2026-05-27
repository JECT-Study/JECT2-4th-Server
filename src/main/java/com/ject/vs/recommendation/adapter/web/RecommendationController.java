package com.ject.vs.recommendation.adapter.web;

import com.ject.vs.recommendation.port.in.RecommendationCommandUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "추천 투표", description = "오늘의 추천 투표 관리 API")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationCommandUseCase recommendationCommandUseCase;

    @Operation(summary = "오늘의 추천 투표 설정", description = "운영진이 오늘의 추천 투표를 설정합니다.")
    @PostMapping
    public void setTodayRecommendations(
            @AuthenticationPrincipal Long userId,
            @RequestBody SetRecommendationRequest request
    ) {
        recommendationCommandUseCase.setTodayRecommendations(userId, request.voteIds());
    }

    public record SetRecommendationRequest(List<Long> voteIds) {}
}
