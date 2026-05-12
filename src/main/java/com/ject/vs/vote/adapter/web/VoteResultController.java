package com.ject.vs.vote.adapter.web;

import com.ject.vs.vote.adapter.web.dto.ShareLinkResponse;
import com.ject.vs.vote.adapter.web.dto.VoteResultResponse;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes/{voteId}")
@RequiredArgsConstructor
public class VoteResultController {

    private final VoteResultQueryUseCase voteResultQueryUseCase;

    @GetMapping("/result")
    public VoteResultResponse getResult(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId) {
        return VoteResultResponse.from(voteResultQueryUseCase.getResult(voteId, userId));
    }

    @GetMapping("/share")
    public ShareLinkResponse getShareLink(@PathVariable Long voteId) {
        return new ShareLinkResponse(voteResultQueryUseCase.getShareLink(voteId).url());
    }
}
