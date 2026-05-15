package com.ject.vs.auth.port.in.dto;

import com.ject.vs.auth.domain.TokenType;

import java.time.LocalDateTime;

public record TokenInfo(String tokenValue, TokenType tokenType, LocalDateTime expiresAt, Long userId) {
    public boolean isAccessToken() {
        return TokenType.ACCESS.equals(tokenType);
    }
}
