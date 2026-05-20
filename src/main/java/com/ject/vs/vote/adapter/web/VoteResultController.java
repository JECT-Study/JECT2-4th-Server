package com.ject.vs.vote.adapter.web;

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

    private final VoteResultQueryUseCase voteResultQueryUseCase;

    @Operation(summary = "투표 결과 조회", description = "마감된 투표의 결과를 조회합니다. 진행 중 투표는 403 응답합니다. 비회원은 insight가 잠금 상태로 응답됩니다.")
    @GetMapping("/result")
    public VoteResultResponse getResult(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId) {
        return VoteResultResponse.from(voteResultQueryUseCase.getResult(voteId, userId));
    }

    @Operation(summary = "공유 링크 생성", description = "투표 공유를 위한 링크를 생성합니다.")
    @GetMapping("/share")
    public ShareLinkResponse getShareLink(@PathVariable Long voteId) {
        return ShareLinkResponse.from(voteResultQueryUseCase.getShareLink(voteId));
    }
}
