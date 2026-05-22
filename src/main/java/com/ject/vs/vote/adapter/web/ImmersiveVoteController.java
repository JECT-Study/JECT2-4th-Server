package com.ject.vs.vote.adapter.web;

import com.ject.vs.config.AnonymousId;
import com.ject.vs.vote.adapter.web.dto.ImmersiveFeedResponse;
import com.ject.vs.vote.adapter.web.dto.ImmersiveLiveResponse;
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

    private final ImmersiveVoteCommandUseCase immersiveVoteCommandUseCase;
    private final ImmersiveVoteQueryUseCase immersiveVoteQueryUseCase;
    private final VoteResultQueryUseCase voteResultQueryUseCase;

    @Operation(summary = "몰입형 투표 피드 조회", description = "스와이프 형식의 몰입형 투표 피드를 조회합니다. 커서 기반 페이지네이션을 지원합니다.")
    @GetMapping
    public ImmersiveFeedResponse getFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @AnonymousId String anonymousId) {
        return ImmersiveFeedResponse.from(immersiveVoteQueryUseCase.getFeed(cursor, size, userId, anonymousId));
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
        return ImmersiveParticipateResponse.from(result);
    }

    @Operation(summary = "실시간 투표 현황 조회", description = "투표 후 실시간 비율 갱신을 위한 폴링 API입니다.")
    @GetMapping("/{voteId}/live")
    public ImmersiveLiveResponse getLive(@PathVariable Long voteId) {
        return ImmersiveLiveResponse.from(immersiveVoteQueryUseCase.getLive(voteId));
    }

    @Operation(summary = "공유 링크 생성", description = "투표 공유를 위한 링크를 생성합니다.")
    @GetMapping("/{voteId}/share")
    public ShareLinkResponse getShareLink(@PathVariable Long voteId) {
        return ShareLinkResponse.from(voteResultQueryUseCase.getShareLink(voteId));
    }
}
