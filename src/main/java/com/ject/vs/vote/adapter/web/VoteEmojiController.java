package com.ject.vs.vote.adapter.web;

import com.ject.vs.config.AnonymousId;
import com.ject.vs.vote.adapter.web.dto.EmojiRequest;
import com.ject.vs.vote.adapter.web.dto.EmojiResponse;
import com.ject.vs.vote.port.in.VoteEmojiCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class VoteEmojiController {

    private final VoteEmojiCommandUseCase voteEmojiCommandUseCase;

    @PutMapping("/api/votes/{voteId}/emoji")
    public EmojiResponse reactOnVote(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @AnonymousId String anonymousId,
            @RequestBody EmojiRequest request) {
        VoteEmojiCommandUseCase.EmojiResult result = userId != null
                ? voteEmojiCommandUseCase.reactAsMember(voteId, userId, request.emoji())
                : voteEmojiCommandUseCase.reactAsGuest(voteId, anonymousId, request.emoji());
        return EmojiResponse.from(result);
    }

    @PutMapping("/api/immersive-votes/{voteId}/emoji")
    public EmojiResponse reactOnImmersiveVote(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @AnonymousId String anonymousId,
            @RequestBody EmojiRequest request) {
        VoteEmojiCommandUseCase.EmojiResult result = userId != null
                ? voteEmojiCommandUseCase.reactAsMember(voteId, userId, request.emoji())
                : voteEmojiCommandUseCase.reactAsGuest(voteId, anonymousId, request.emoji());
        return EmojiResponse.from(result);
    }
}
