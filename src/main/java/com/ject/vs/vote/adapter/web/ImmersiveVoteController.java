package com.ject.vs.vote.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.config.AnonymousId;
import com.ject.vs.vote.adapter.web.dto.ImmersiveFeedResponse;
import com.ject.vs.vote.adapter.web.dto.ImmersiveLiveResponse;
import com.ject.vs.vote.adapter.web.dto.ImmersiveNextRequest;
import com.ject.vs.vote.adapter.web.dto.ImmersiveNextResponse;
import com.ject.vs.vote.adapter.web.dto.ImmersiveParticipateResponse;
import com.ject.vs.vote.adapter.web.dto.ParticipateRequest;
import com.ject.vs.vote.adapter.web.dto.ShareLinkResponse;
import com.ject.vs.vote.domain.VoteSortType;
import com.ject.vs.vote.port.in.ImmersiveVoteCommandUseCase;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "몰입형 투표", description = "몰입형 투표 피드 관련 API")
@RestController
@RequestMapping("/api/immersive-votes")
@RequiredArgsConstructor
public class ImmersiveVoteController {

    private static final String VOTE_TYPE_IMMERSIVE = "IMMERSIVE";

    private final ImmersiveVoteCommandUseCase immersiveVoteCommandUseCase;
    private final ImmersiveVoteQueryUseCase immersiveVoteQueryUseCase;
    private final VoteResultQueryUseCase voteResultQueryUseCase;
    private final AnalyticsEventLogger analytics;

    @Operation(summary = "몰입형 투표 피드 조회", description = "스와이프 형식의 몰입형 투표 피드를 조회합니다. 커서 기반 페이지네이션을 지원합니다. startVoteId를 지정하면 해당 투표부터 피드가 시작됩니다.")
    @GetMapping
    public ImmersiveFeedResponse getFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Long startVoteId,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @AnonymousId String anonymousId) {
        ImmersiveFeedResponse response = ImmersiveFeedResponse.from(
                immersiveVoteQueryUseCase.getFeed(cursor, startVoteId, size, userId, anonymousId));

        analytics.log(AnalyticsEvent.of("immersive_feed_viewed")
                .anonymousId(anonymousId)
                .put("loaded_vote_count", response.votes().size())
                .put("next_cursor", response.nextCursor())
                .put("has_next", response.hasNext()));

        return response;
    }

    @Operation(summary = "투표 참여/취소", description = "투표에 참여하거나 같은 옵션 재클릭 시 취소합니다. 비회원은 5회까지 무료 투표 가능합니다.")
    @PostMapping("/{voteId}/participate")
    public ImmersiveParticipateResponse participateOrCancel(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @AnonymousId String anonymousId,
            @RequestBody @Valid ParticipateRequest request) {
        ImmersiveVoteCommandUseCase.ImmersiveParticipateResult result =
                immersiveVoteCommandUseCase.participateOrCancel(voteId, userId, anonymousId, request.optionId());

        analytics.log(AnalyticsEvent.of("immersive_vote_participated")
                .anonymousId(anonymousId)
                .put("vote_id", result.voteId())
                .put("option_id", request.optionId())
                .put("action", result.action())
                .put("selected_option_id", result.selectedOptionId())
                .put("remaining_free_votes", result.remainingFreeVotes())
                .put("vote_type", VOTE_TYPE_IMMERSIVE));

        return ImmersiveParticipateResponse.from(result);
    }

    @Operation(summary = "실시간 투표 현황 조회", description = "투표 후 실시간 비율 갱신을 위한 폴링 API입니다.")
    @GetMapping("/{voteId}/live")
    public ImmersiveLiveResponse getLive(@PathVariable Long voteId) {
        ImmersiveLiveResponse response = ImmersiveLiveResponse.from(immersiveVoteQueryUseCase.getLive(voteId));

        analytics.log(AnalyticsEvent.of("immersive_live_viewed")
                .put("vote_id", voteId)
                .put("current_viewer_count", response.currentViewerCount())
                .put("total_participant_count", response.totalParticipantCount()));

        return response;
    }

    @Operation(summary = "공유 링크 생성", description = "투표 공유를 위한 링크를 생성합니다.")
    @GetMapping("/{voteId}/share")
    public ShareLinkResponse getShareLink(@PathVariable Long voteId) {
        ShareLinkResponse response = ShareLinkResponse.from(voteResultQueryUseCase.getShareLink(voteId));

        analytics.log(AnalyticsEvent.of("share_link_generated")
                .put("vote_id", voteId)
                .put("share_url", response.shareUrl())
                .put("title", response.title())
                .put("thumbnail_url", response.thumbnailUrl())
                .put("vote_type", VOTE_TYPE_IMMERSIVE));

        return response;
    }

    @Operation(summary = "랜덤 다음 투표 조회", description = "excludeIds를 제외한 진행 중인 투표를 랜덤으로 조회합니다. 모든 투표 소진 시 빈 배열 반환 → 클라이언트에서 excludeIds 초기화 후 재요청 (무한 순환). startVoteId를 지정하면 진행 중인 해당 투표가 맨 앞에 배치되고 나머지가 랜덤으로 채워집니다.")
    @PostMapping("/next")
    public ImmersiveNextResponse getNextRandom(
            @RequestBody @Valid ImmersiveNextRequest request,
            @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @AnonymousId String anonymousId) {
        return ImmersiveNextResponse.from(
                immersiveVoteQueryUseCase.getNextRandom(
                        request.excludeIds(), request.startVoteId(), request.size(), userId, anonymousId)
        );
    }
}
