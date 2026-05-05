package com.ject.vs.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    USER_NOT_FOUND("E400000", "사용자 정보가 없습니다.", HttpStatus.NOT_FOUND),
    EXPIRED_TOKEN("E400001", "인증되지 않은 사용자입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("E400002", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
