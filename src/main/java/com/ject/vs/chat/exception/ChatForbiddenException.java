package com.ject.vs.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ChatForbiddenException extends RuntimeException {
    public ChatForbiddenException() {
        super("채팅방에 참여하지 않은 사용자입니다.");
    }
}
