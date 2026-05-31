package com.ject.vs.user.exception;

import com.ject.vs.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("E400000", "사용자 정보가 없습니다.", 404),
    USER_NOT_REGISTER("E400001", "등록되지 않은 사용자입니다.", 404),
    REREGISTRATION_RESTRICTED("E400002", "탈퇴 후 30일 이내에는 동일 이메일로 재가입할 수 없습니다.", 409);

    private final String code;
    private final String message;
    private final Integer statusCode;
}
