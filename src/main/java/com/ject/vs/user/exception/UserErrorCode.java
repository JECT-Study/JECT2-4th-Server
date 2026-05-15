package com.ject.vs.user.exception;

import com.ject.vs.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("E400000", "사용자 정보가 없습니다.", 404);

    private final String code;
    private final String message;
    private final Integer statusCode;
}
