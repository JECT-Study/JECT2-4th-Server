package com.ject.vs.vote.exception;

public class InvalidOptionException extends RuntimeException {

    public InvalidOptionException() {
        super("유효하지 않은 투표 선택지입니다.");
    }

    public String getErrorCode() {
        return "INVALID_OPTION";
    }
}
