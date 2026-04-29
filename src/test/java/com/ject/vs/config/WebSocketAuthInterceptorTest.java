package com.ject.vs.config;

import com.ject.vs.util.JwtProvider;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private MessageChannel messageChannel;

    private Message<?> buildConnectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) {
            accessor.setNativeHeader("Authorization", authHeader);
        }
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
        void CONNECT_유효한_토큰이면_Principal이_설정된다() {
            // given
            given(jwtProvider.getUserId("valid-token")).willReturn(42L);
            Message<?> message = buildConnectMessage("Bearer valid-token");

            // when
            Message<?> result = interceptor.preSend(message, messageChannel);

            // then
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            assertThat(accessor.getUser()).isNotNull();
            assertThat(accessor.getUser().getName()).isEqualTo("42");
        }

        @Test
        void CONNECT_토큰_없으면_Principal이_null이다() {
            // given
            Message<?> message = buildConnectMessage(null);

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
