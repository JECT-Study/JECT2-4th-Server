package com.ject.vs.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMessageException extends RuntimeException {
    public InvalidMessageException() {
        super("메시지 내용이 비어 있습니다.");
    }
}
