package com.ject.vs.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    @Mock
    private MessageChannel messageChannel;

    private Message<?> buildConnectMessageWithSessionUserId(Long userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (userId != null) {
            accessor.setSessionAttributes(Map.of("userId", userId));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildConnectMessageWithoutSession() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildNonConnectMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Nested
    class preSend {

        @Test
        void CONNECT_세션에_userId가_있으면_Principal이_설정된다() {
            // given (HandshakeInterceptor가 쿠키에서 userId를 추출해 sessionAttributes에 저장한 상태)
            Message<?> message = buildConnectMessageWithSessionUserId(42L);

            // when
            Message<?> result = interceptor.preSend(message, messageChannel);

            // then
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            assertThat(accessor.getUser()).isNotNull();
            assertThat(accessor.getUser().getName()).isEqualTo("42");
        }

        @Test
        void CONNECT_세션에_userId가_없으면_Principal이_null이다() {
            // given (쿠키 없음 또는 유효하지 않은 토큰 → anonymous)
            Message<?> message = buildConnectMessageWithoutSession();

            // when
            Message<?> result = interceptor.preSend(message, messageChannel);

            // then
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            assertThat(accessor.getUser()).isNull();
        }

        @Test
        void CONNECT_아닌_frame은_그냥_통과된다() {
            // given
            Message<?> message = buildNonConnectMessage();

            // when
            Message<?> result = interceptor.preSend(message, messageChannel);

            // then
            assertThat(result).isNotNull();
        }
    }
}
