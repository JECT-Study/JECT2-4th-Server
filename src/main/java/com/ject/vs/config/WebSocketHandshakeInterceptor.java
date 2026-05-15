package com.ject.vs.config;

import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final CookieUtil cookieUtil;
    private final JwtProvider jwtProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String accessToken = cookieUtil.getCookieValue(httpRequest, CookieUtil.CookieType.ACCESS_TOKEN);

            if (accessToken != null && !accessToken.isBlank()) {
                try {
                    Long userId = jwtProvider.getUserId(accessToken);
                    attributes.put("userId", userId);
                    log.debug("WebSocket handshake authenticated userId={}", userId);
                } catch (Exception e) {
                    log.debug("WebSocket handshake token validation failed: {}", e.getMessage());
                    // invalid token -> anonymous connection (no userId in attributes)
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
