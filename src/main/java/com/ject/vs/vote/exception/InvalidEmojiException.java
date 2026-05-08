package com.ject.vs.vote.exception;

public class InvalidEmojiException extends RuntimeException {

    public InvalidEmojiException() {
        super("유효하지 않은 이모지입니다.");
    }

    public String getErrorCode() {
        return "INVALID_EMOJI";
    }
}
