package com.ject.vs.vote.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.vote.adapter.web.dto.ShareLinkResponse;
import com.ject.vs.vote.adapter.web.dto.VoteResultResponse;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "투표 결과", description = "마감된 투표 결과 조회 API")
@RestController
@RequestMapping("/api/votes/{voteId}")
@RequiredArgsConstructor
public class VoteResultController {

    private static final String VOTE_TYPE_GENERAL = "GENERAL";

    private final VoteResultQueryUseCase voteResultQueryUseCase;
    private final AnalyticsEventLogger analytics;

    @Operation(summary = "투표 결과 조회", description = "마감된 투표의 결과를 조회합니다. 진행 중 투표는 403 응답합니다. 비회원은 insight가 잠금 상태로 응답됩니다.")
    @GetMapping("/result")
    public VoteResultResponse getResult(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId) {
        VoteResultResponse response = VoteResultResponse.from(voteResultQueryUseCase.getResult(voteId, userId));

        var insight = response.insight();
        var aiInsight = response.aiInsight();
        analytics.log(AnalyticsEvent.of("result_page_viewed")
                .put("vote_id", response.voteId())
                .put("vote_status", response.status())
                .put("participant_count", response.participantCount())
                .put("my_vote_voted", response.myVote().voted())
                .put("selected_option_id", response.myVote().selectedOptionId())
                .put("insight_locked", insight != null ? insight.locked() : null)
                .put("insight_scope", insight != null ? insight.scope() : null)
                .put("selection_count", insight != null ? insight.selectionCount() : null)
                .put("ai_insight_available", aiInsight != null && aiInsight.available()));

        return response;
    }

    @Operation(summary = "공유 링크 생성", description = "투표 공유를 위한 링크를 생성합니다.")
    @GetMapping("/share")
    public ShareLinkResponse getShareLink(@PathVariable Long voteId) {
        ShareLinkResponse response = ShareLinkResponse.from(voteResultQueryUseCase.getShareLink(voteId));

        analytics.log(AnalyticsEvent.of("share_link_generated")
                .put("vote_id", voteId)
                .put("share_url", response.shareUrl())
                .put("title", response.title())
                .put("thumbnail_url", response.thumbnailUrl())
                .put("vote_type", VOTE_TYPE_GENERAL));

        return response;
    }
}
