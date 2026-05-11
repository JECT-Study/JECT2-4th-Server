package com.ject.vs.dto;

import com.ject.vs.domain.Token;
import com.ject.vs.domain.TokenType;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * @param tokenValue 값이 변하지 않기 위해 final 키워드 사용
 */
public record TokenInfo(String tokenValue, TokenType tokenType, LocalDateTime expiresAt, Long userId) {
    public boolean isAccessToken() {
        return TokenType.ACCESS.equals(tokenType);
    }
}
