package com.ject.vs.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ChatMessageNotFoundException extends RuntimeException {
    public ChatMessageNotFoundException() {
        super("채팅 메시지를 찾을 수 없습니다.");
    }
}