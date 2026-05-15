package com.ject.vs.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && sessionAttributes.containsKey("userId")) {
                Long userId = (Long) sessionAttributes.get("userId");
                accessor.setUser(() -> String.valueOf(userId));
                log.debug("WebSocket STOMP CONNECT authenticated via cookie, userId={}", userId);
            }
            // 쿠키 기반 인증 외에는 Principal을 설정하지 않음 (anonymous)
        }

        return message;
    }
}
