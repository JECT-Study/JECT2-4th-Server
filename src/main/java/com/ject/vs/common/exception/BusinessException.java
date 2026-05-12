package com.ject.vs.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return this.errorCode.getCode();
    }

    public String getErrorMessage() {
        return this.errorCode.getMessage();
    }

    public Integer getStatusCode() {
        return this.errorCode.getStatusCode();
    }
}
