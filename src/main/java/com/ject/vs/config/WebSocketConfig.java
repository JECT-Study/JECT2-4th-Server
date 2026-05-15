package com.ject.vs.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;
    private final CorsProperties corsProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOriginPatterns = corsProperties.allowedOriginPatterns().toArray(String[]::new);
        if (allowedOriginPatterns.length > 0) {
            registry.addEndpoint("/ws")
                    .addInterceptors(webSocketHandshakeInterceptor)
                    .setAllowedOriginPatterns(allowedOriginPatterns)
                    .withSockJS();
            return;
        }

        registry.addEndpoint("/ws")
                .addInterceptors(webSocketHandshakeInterceptor)
                .setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        // 사용자별 개인 채널이 필요할 경우 convertAndSendToUser + /user prefix를 사용했으나,
        // 현재는 /topic/chat/{voteId}/unread/{userId} 경로로 통일하여 /user prefix를 제거함.
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
