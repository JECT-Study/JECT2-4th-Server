package com.ject.vs.vote.exception;

public class UnauthorizedException extends RuntimeException {

    private static final String ERROR_CODE = "UNAUTHORIZED";

    public UnauthorizedException() {
        super("인증이 필요합니다");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
