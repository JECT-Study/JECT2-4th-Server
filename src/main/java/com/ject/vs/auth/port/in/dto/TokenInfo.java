package com.ject.vs.auth.port.in.dto;

import com.ject.vs.auth.domain.TokenType;

import java.time.Instant;

public record TokenInfo(String tokenValue, TokenType tokenType, Instant expiresAt, Long userId) {
    public boolean isAccessToken() {
        return TokenType.ACCESS.equals(tokenType);
    }
}
