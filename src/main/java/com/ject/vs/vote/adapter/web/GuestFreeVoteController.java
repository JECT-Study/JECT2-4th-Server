package com.ject.vs.vote.adapter.web;

import com.ject.vs.config.AnonymousId;
import com.ject.vs.vote.adapter.web.dto.FreeVotesResponse;
import com.ject.vs.vote.domain.GuestFreeVote;
import com.ject.vs.vote.port.GuestFreeVoteService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class GuestFreeVoteController {

    private final GuestFreeVoteService guestFreeVoteService;

    @GetMapping("/free-votes")
    public FreeVotesResponse getFreeVotes(@Parameter(hidden = true) @AnonymousId String anonymousId) {
        return new FreeVotesResponse(
                guestFreeVoteService.remaining(anonymousId),
                GuestFreeVote.totalFreeVotes()
        );
    }
}
