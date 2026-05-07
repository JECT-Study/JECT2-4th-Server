package com.ject.vs.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    USER_NOT_FOUND("E400000", "사용자 정보가 없습니다.", HttpStatus.NOT_FOUND),
    EXPIRED_TOKEN("E400001", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("E400002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND("E400003", "토큰이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("E400004", "재발급 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN_TYPE("E400005", "해당 토큰의 종류가 다릅니다.", HttpStatus.UNAUTHORIZED),
    REVOKED_TOKEN("E400006", "회수된 토큰입니다.", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
