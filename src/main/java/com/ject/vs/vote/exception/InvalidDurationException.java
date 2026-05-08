package com.ject.vs.vote.exception;

public class InvalidDurationException extends RuntimeException {

    public InvalidDurationException(int hours) {
        super("유효하지 않은 투표 기간입니다: " + hours + "시간");
    }

    public String getErrorCode() {
        return "INVALID_DURATION";
    }
}
