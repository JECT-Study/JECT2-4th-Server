package com.ject.vs.auth.exception;

import com.ject.vs.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenErrorCode implements ErrorCode {
    EXPIRED_TOKEN("E400001", "토큰이 만료되었습니다.", 401),
    INVALID_TOKEN("E400002", "유효하지 않은 토큰입니다.", 401),
    INVALID_TOKEN_TYPE("E400005", "해당 토큰의 종류가 다릅니다.", 401),
    REFRESH_TOKEN_EXPIRED("E400004", "재발급 토큰이 만료되었습니다.", 401),
    REVOKED_TOKEN("E400006", "회수된 토큰입니다.", 401),
    TOKEN_NOT_FOUND("E400003", "토큰이 존재하지 않습니다.", 401);

    private final String code;
    private final String message;
    private final Integer statusCode;
}
