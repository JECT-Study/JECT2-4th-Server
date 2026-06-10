package com.ject.vs.vote.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "일반형 투표", description = "일반형 투표 관련 API")
@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private static final String VOTE_TYPE_GENERAL = "GENERAL";

    private final VoteCommandUseCase voteCommandUseCase;
    private final VoteDetailQueryService voteDetailQueryService;
    private final VoteParticipationQueryUseCase voteParticipationQueryUseCase;
    private final AnalyticsEventLogger analytics;

    @Operation(summary = "투표 생성", description = "새로운 투표를 생성합니다. 회원만 가능합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VoteCreateResponse create(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VoteCreateRequest request) {
        if (userId == null) throw new UnauthorizedException();
        return VoteCreateResponse.from(voteCommandUseCase.create(request.toCommand()));
    }

    @Operation(summary = "투표 생성 (이미지 포함)", description = "이미지 파일과 함께 투표를 생성합니다. 서버에서 S3 업로드를 처리합니다.")
    @PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VoteCreateResponse createWithImages(
            @AuthenticationPrincipal Long userId,
            @RequestParam("title") String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam("thumbnailFile") MultipartFile thumbnailFile,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam("duration") String duration,
            @RequestParam("optionA") String optionA,
            @RequestParam("optionB") String optionB) {
        if (userId == null) throw new UnauthorizedException();

        var command = new VoteCommandUseCase.VoteCreateWithImagesCommand(
                title, content, thumbnailFile, imageFile,
                com.ject.vs.vote.domain.VoteDuration.valueOf(duration),
                optionA, optionB
        );
        return VoteCreateResponse.from(voteCommandUseCase.createWithImages(command));
    }

    @Operation(summary = "투표 상세 조회", description = "투표 상세 정보를 조회합니다. 투표 전에는 결과(voteCount/ratio)가 null로 응답됩니다.")
    @GetMapping("/{voteId}")
    public VoteDetailResponse getDetail(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @AnonymousId String anonymousId) {
        VoteDetailResponse response = VoteDetailResponse.from(voteDetailQueryService.getDetail(voteId, userId, anonymousId));

        analytics.log(AnalyticsEvent.of("vote_detail_viewed")
                .anonymousId(anonymousId)
                .put("vote_id", response.voteId())
                .put("vote_status", response.status())
                .put("participant_count", response.participantCount())
                .put("my_vote_voted", response.myVote().voted())
                .put("selected_option_id", response.myVote().selectedOptionId())
                .put("vote_type", VOTE_TYPE_GENERAL));

        return response;
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

        analytics.log(AnalyticsEvent.of("vote_participated")
                .anonymousId(anonymousId)
                .put("vote_id", result.voteId())
                .put("option_id", request.optionId())
                .put("selected_option_id", result.selectedOptionId())
                .put("participant_count", result.participantCount())
                .put("remaining_free_votes", result.remainingFreeVotes())
                .put("vote_type", VOTE_TYPE_GENERAL));

        return ParticipateResponse.from(result);
    }

    @Operation(summary = "다시 투표하기", description = "투표를 취소합니다. 회원만 가능합니다.")
    @DeleteMapping("/{voteId}/participate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        Long previousOptionId = voteCommandUseCase.cancel(voteId, userId);

        analytics.log(AnalyticsEvent.of("vote_canceled")
                .put("vote_id", voteId)
                .put("previous_option_id", previousOptionId)
                .put("vote_type", VOTE_TYPE_GENERAL));
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
