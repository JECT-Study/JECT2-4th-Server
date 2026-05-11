package com.ject.vs.dto;


import com.ject.vs.domain.Token;
import lombok.Builder;

@Builder
public record TokenReissueResponse(Long userId, String accessToken, String refreshToken) {
    public static TokenReissueResponse from(Token token, String accessToken) {
        return TokenReissueResponse.builder()
                .userId(token.getUser().getId())
                .accessToken(accessToken)
                .refreshToken(token.getTokenValue())
                .build();
    }
}
