package com.ject.vs.vote.adapter.web;

import com.ject.vs.config.AnonymousId;
import com.ject.vs.vote.adapter.web.dto.ImmersiveFeedResponse;
import com.ject.vs.vote.adapter.web.dto.ImmersiveLiveResponse;
import com.ject.vs.vote.adapter.web.dto.ImmersiveParticipateResponse;
import com.ject.vs.vote.adapter.web.dto.ParticipateRequest;
import com.ject.vs.vote.port.in.ImmersiveVoteCommandUseCase;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/immersive-votes")
@RequiredArgsConstructor
public class ImmersiveVoteController {

    private final ImmersiveVoteCommandUseCase immersiveVoteCommandUseCase;
    private final ImmersiveVoteQueryUseCase immersiveVoteQueryUseCase;

    @GetMapping
    public ImmersiveFeedResponse getFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Long userId,
            @AnonymousId String anonymousId) {
        return ImmersiveFeedResponse.from(immersiveVoteQueryUseCase.getFeed(cursor, size, userId, anonymousId));
    }

    @PutMapping("/{voteId}/participate")
    public ImmersiveParticipateResponse participateOrCancel(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @AnonymousId String anonymousId,
            @RequestBody @Valid ParticipateRequest request) {
        ImmersiveVoteCommandUseCase.ImmersiveParticipateResult result =
                immersiveVoteCommandUseCase.participateOrCancel(voteId, userId, anonymousId, request.optionId());
        return ImmersiveParticipateResponse.from(result);
    }

    @GetMapping("/{voteId}/live")
    public ImmersiveLiveResponse getLive(@PathVariable Long voteId) {
        return ImmersiveLiveResponse.from(immersiveVoteQueryUseCase.getLive(voteId));
    }
}
