package com.ject.vs.vote.adapter.web;

import com.ject.vs.config.AnonymousId;
import com.ject.vs.vote.adapter.web.dto.*;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import com.ject.vs.vote.domain.VoteSortType;
import com.ject.vs.vote.exception.UnauthorizedException;
import com.ject.vs.vote.port.VoteDetailQueryService;
import com.ject.vs.vote.port.in.VoteCommandUseCase;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteCommandUseCase voteCommandUseCase;
    private final VoteDetailQueryService voteDetailQueryService;
    private final VoteParticipationQueryUseCase voteParticipationQueryUseCase;
    private final VoteParticipationRepository voteParticipationRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VoteCreateResponse create(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VoteCreateRequest request) {
        if (userId == null) throw new UnauthorizedException();
        return VoteCreateResponse.from(voteCommandUseCase.create(request.toCommand()));
    }

    @GetMapping("/{voteId}")
    public VoteDetailResponse getDetail(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @AnonymousId String anonymousId) {
        return VoteDetailResponse.from(voteDetailQueryService.getDetail(voteId, userId, anonymousId));
    }

    @PostMapping("/{voteId}/participate")
    public ParticipateResponse participate(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @AnonymousId String anonymousId,
            @RequestBody @Valid ParticipateRequest request) {
        VoteCommandUseCase.ParticipateResult result = userId != null
                ? voteCommandUseCase.participateAsMember(voteId, userId, request.optionId())
                : voteCommandUseCase.participateAsGuest(voteId, anonymousId, request.optionId());
        return ParticipateResponse.from(result);
    }

    @DeleteMapping("/{voteId}/participate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        voteCommandUseCase.cancel(voteId, userId);
    }

    @GetMapping("/me/participated")
    public MyParticipatedVoteResponse getVoteListParticipated(@AuthenticationPrincipal Long userId, @RequestParam VoteSortType type) {
        return voteParticipationQueryUseCase.findVotesByOrder(userId, type);
    }

    @GetMapping("/me/participated/end")
    public MyParticipatedVoteResponse getVoteListEndParticipated(@AuthenticationPrincipal Long userId, @RequestParam VoteSortType type) {
        return voteParticipationQueryUseCase.findVotesEndByOrder(userId, type);
    }
}
