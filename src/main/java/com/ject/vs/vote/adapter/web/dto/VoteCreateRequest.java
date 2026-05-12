package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.VoteDuration;
import com.ject.vs.vote.domain.VoteType;
import com.ject.vs.vote.port.in.VoteCommandUseCase.VoteCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VoteCreateRequest(
        @NotNull VoteType type,
        @NotBlank @Size(max = 100) String title,
        @Size(max = 1000) String content,
        @NotBlank String thumbnailUrl,
        String imageUrl,
        @NotNull VoteDuration duration,
        @NotBlank @Size(max = 50) String optionA,
        @NotBlank @Size(max = 50) String optionB
) {
    public VoteCreateCommand toCommand() {
        return new VoteCreateCommand(type, title, content, thumbnailUrl, imageUrl, duration, optionA, optionB);
    }
}
