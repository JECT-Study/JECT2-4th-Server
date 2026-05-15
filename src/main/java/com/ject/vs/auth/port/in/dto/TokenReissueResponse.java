package com.ject.vs.auth.port.in.dto;

import com.ject.vs.auth.domain.Token;
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
