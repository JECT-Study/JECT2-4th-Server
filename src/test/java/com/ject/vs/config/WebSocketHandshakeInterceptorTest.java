package com.ject.vs.config;

import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WebSocketHandshakeInterceptorTest {

    @InjectMocks
    private WebSocketHandshakeInterceptor interceptor;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler webSocketHandler;

    private ServerHttpRequest servletRequestWithAccessTokenCookie(String token) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setCookies(new Cookie(CookieUtil.CookieType.ACCESS_TOKEN, token));
        return new ServletServerHttpRequest(servletRequest);
    }

    @Nested
    class beforeHandshake {

        @Test
        void 유효한_accessToken_쿠키가_있으면_session_attribute에_userId를_저장하고_연결을_허용한다() {
            // given
            String token = "valid.jwt";
            ServerHttpRequest request = servletRequestWithAccessTokenCookie(token);
            Map<String, Object> attributes = new HashMap<>();
            given(cookieUtil.getCookieValue(
                    ((ServletServerHttpRequest) request).getServletRequest(),
                    CookieUtil.CookieType.ACCESS_TOKEN
            )).willReturn(token);
            given(jwtProvider.getUserId(token)).willReturn(42L);

            // when
            boolean result = interceptor.beforeHandshake(request, response, webSocketHandler, attributes);

            // then
            assertThat(result).isTrue();
            assertThat(attributes).containsEntry("userId", 42L);
        }

        @Test
        void accessToken_쿠키가_없으면_anonymous로_연결을_허용한다() {
            // given
            ServerHttpRequest request = servletRequestWithAccessTokenCookie("");
            Map<String, Object> attributes = new HashMap<>();
            given(cookieUtil.getCookieValue(
                    ((ServletServerHttpRequest) request).getServletRequest(),
                    CookieUtil.CookieType.ACCESS_TOKEN
            )).willReturn(null);

            // when
            boolean result = interceptor.beforeHandshake(request, response, webSocketHandler, attributes);

            // then
            assertThat(result).isTrue();
            assertThat(attributes).doesNotContainKey("userId");
        }

        @Test
        void accessToken이_blank이면_anonymous로_연결을_허용한다() {
            // given
            ServerHttpRequest request = servletRequestWithAccessTokenCookie(" ");
            Map<String, Object> attributes = new HashMap<>();
            given(cookieUtil.getCookieValue(
                    ((ServletServerHttpRequest) request).getServletRequest(),
                    CookieUtil.CookieType.ACCESS_TOKEN
            )).willReturn(" ");

            // when
            boolean result = interceptor.beforeHandshake(request, response, webSocketHandler, attributes);

            // then
            assertThat(result).isTrue();
            assertThat(attributes).doesNotContainKey("userId");
        }

        @Test
        void token_검증이_실패해도_anonymous로_연결을_허용한다() {
            // given
            String token = "invalid.jwt";
            ServerHttpRequest request = servletRequestWithAccessTokenCookie(token);
            Map<String, Object> attributes = new HashMap<>();
            given(cookieUtil.getCookieValue(
                    ((ServletServerHttpRequest) request).getServletRequest(),
                    CookieUtil.CookieType.ACCESS_TOKEN
            )).willReturn(token);
            given(jwtProvider.getUserId(token)).willThrow(new IllegalArgumentException("invalid token"));

            // when
            boolean result = interceptor.beforeHandshake(request, response, webSocketHandler, attributes);

            // then
            assertThat(result).isTrue();
            assertThat(attributes).doesNotContainKey("userId");
        }

        @Test
        void servlet_request가_아니어도_연결을_허용한다() {
            // given
            ServerHttpRequest request = org.mockito.Mockito.mock(ServerHttpRequest.class);
            Map<String, Object> attributes = new HashMap<>();

            // when
            boolean result = interceptor.beforeHandshake(request, response, webSocketHandler, attributes);

            // then
            assertThat(result).isTrue();
            assertThat(attributes).isEmpty();
        }
    }
}
