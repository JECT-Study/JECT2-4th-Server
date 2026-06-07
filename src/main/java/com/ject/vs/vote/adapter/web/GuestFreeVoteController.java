package com.ject.vs.vote.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.config.AnonymousId;
import com.ject.vs.vote.adapter.web.dto.FreeVotesResponse;
import com.ject.vs.vote.domain.GuestFreeVote;
import com.ject.vs.vote.port.GuestFreeVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "비회원 무료 투표", description = "비회원 무료 투표권 관련 API")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class GuestFreeVoteController {

    private final GuestFreeVoteService guestFreeVoteService;
    private final AnalyticsEventLogger analytics;

    @Operation(summary = "잔여 무료 투표권 조회", description = "비회원의 잔여 무료 투표권 수를 조회합니다. 회원은 remainingFreeVotes가 null로 응답됩니다.")
    @GetMapping("/free-votes")
    public FreeVotesResponse getFreeVotes(
            @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @AnonymousId String anonymousId) {
        // 회원은 무료 투표권 제한이 없으므로 null로 응답
        FreeVotesResponse response = (userId != null)
                ? new FreeVotesResponse(null, null)
                : new FreeVotesResponse(
                        guestFreeVoteService.remaining(anonymousId),
                        GuestFreeVote.totalFreeVotes());

        analytics.log(AnalyticsEvent.of("free_votes_checked")
                .anonymousId(anonymousId)
                .put("remaining_free_votes", response.remainingFreeVotes())
                .put("total_free_votes", response.totalFreeVotes()));

        return response;
    }
}
