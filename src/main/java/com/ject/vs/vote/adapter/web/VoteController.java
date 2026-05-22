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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "일반형 투표", description = "일반형 투표 관련 API")
@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteCommandUseCase voteCommandUseCase;
    private final VoteDetailQueryService voteDetailQueryService;
    private final VoteParticipationQueryUseCase voteParticipationQueryUseCase;
    private final VoteParticipationRepository voteParticipationRepository;

    @Operation(summary = "투표 생성", description = "새로운 투표를 생성합니다. 회원만 가능합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VoteCreateResponse create(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VoteCreateRequest request) {
        if (userId == null) throw new UnauthorizedException();
        return VoteCreateResponse.from(voteCommandUseCase.create(request.toCommand()));
    }

    @Operation(summary = "투표 상세 조회", description = "투표 상세 정보를 조회합니다. 투표 전에는 결과(voteCount/ratio)가 null로 응답됩니다.")
    @GetMapping("/{voteId}")
    public VoteDetailResponse getDetail(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @AnonymousId String anonymousId) {
        return VoteDetailResponse.from(voteDetailQueryService.getDetail(voteId, userId, anonymousId));
    }

    @Operation(summary = "투표 참여", description = "투표에 참여합니다. 비회원은 5회까지 무료 투표 가능합니다.")
    @PostMapping("/{voteId}/participate")
    public ParticipateResponse participate(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @AnonymousId String anonymousId,
            @RequestBody @Valid ParticipateRequest request) {
        VoteCommandUseCase.ParticipateResult result = userId != null
                ? voteCommandUseCase.participateAsMember(voteId, userId, request.optionId())
                : voteCommandUseCase.participateAsGuest(voteId, anonymousId, request.optionId());
        return ParticipateResponse.from(result);
    }

    @Operation(summary = "다시 투표하기", description = "투표를 취소합니다. 회원만 가능합니다.")
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
