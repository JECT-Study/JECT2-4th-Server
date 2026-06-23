package com.ject.vs.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMessageException extends RuntimeException {
    public InvalidMessageException() {
        super("유효하지 않은 메시지 요청입니다.");
    }

    public InvalidMessageException(String message) {
        super(message);
    }
}
