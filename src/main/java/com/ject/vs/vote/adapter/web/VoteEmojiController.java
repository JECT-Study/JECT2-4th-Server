package com.ject.vs.vote.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.config.AnonymousId;
import com.ject.vs.vote.adapter.web.dto.EmojiRequest;
import com.ject.vs.vote.adapter.web.dto.EmojiResponse;
import com.ject.vs.vote.port.in.VoteEmojiCommandUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "이모지 반응", description = "투표 이모지 반응 API")
@RestController
@RequiredArgsConstructor
public class VoteEmojiController {

    private final VoteEmojiCommandUseCase voteEmojiCommandUseCase;
    private final AnalyticsEventLogger analytics;

    @Operation(summary = "일반형 투표 이모지 반응", description = "이모지 반응을 추가/변경/취소합니��. 같은 이모지 재선택 또는 null 전송 시 취소됩니다.")
    @PutMapping("/api/votes/{voteId}/emoji")
    public EmojiResponse reactOnVote(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @AnonymousId String anonymousId,
            @RequestBody EmojiRequest request) {
        VoteEmojiCommandUseCase.EmojiResult result = userId != null
                ? voteEmojiCommandUseCase.reactAsMember(voteId, userId, request.emoji())
                : voteEmojiCommandUseCase.reactAsGuest(voteId, anonymousId, request.emoji());
        logEmojiReacted(voteId, anonymousId, request, result, "GENERAL");
        return EmojiResponse.from(result);
    }

    @Operation(summary = "몰입형 투표 이모지 반응", description = "이모지 반응을 추가/변경/취소합니다. 같은 이모지 재선택 또는 null 전송 시 취소됩니다.")
    @PutMapping("/api/immersive-votes/{voteId}/emoji")
    public EmojiResponse reactOnImmersiveVote(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @AnonymousId String anonymousId,
            @RequestBody EmojiRequest request) {
        VoteEmojiCommandUseCase.EmojiResult result = userId != null
                ? voteEmojiCommandUseCase.reactAsMember(voteId, userId, request.emoji())
                : voteEmojiCommandUseCase.reactAsGuest(voteId, anonymousId, request.emoji());
        logEmojiReacted(voteId, anonymousId, request, result, "IMMERSIVE");
        return EmojiResponse.from(result);
    }

    private void logEmojiReacted(Long voteId, String anonymousId, EmojiRequest request,
                                 VoteEmojiCommandUseCase.EmojiResult result, String voteType) {
        analytics.log(AnalyticsEvent.of("emoji_reacted")
                .anonymousId(anonymousId)
                .put("vote_id", voteId)
                .put("emoji_type", request.emoji())
                .put("my_emoji", result.myEmoji())
                .put("emoji_total_count", result.total())
                .put("action", result.action())
                .put("vote_type", voteType));
    }
}
