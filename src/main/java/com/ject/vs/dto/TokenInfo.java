package com.ject.vs.dto;

import com.ject.vs.domain.TokenType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TokenInfo {
    private final String tokenValue;        // 값이 변하지 않기 위해 final 키워드 사용
    private final TokenType tokenType;
    private final LocalDateTime expiresAt;
}
