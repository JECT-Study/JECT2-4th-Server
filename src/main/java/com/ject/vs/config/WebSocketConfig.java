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
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
