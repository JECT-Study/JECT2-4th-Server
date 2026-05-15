package com.ject.vs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.auth.domain.TokenStatus;
import com.ject.vs.auth.domain.TokenType;
import com.ject.vs.auth.exception.TokenErrorCode;
import com.ject.vs.auth.port.in.dto.TokenInfo;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final CookieUtil cookieUtil = mock(CookieUtil.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JwtAuthFilter filter = new JwtAuthFilter(jwtProvider, cookieUtil, objectMapper);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void skipsJwtProcessingForExcludedPaths() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verifyNoInteractions(cookieUtil, jwtProvider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void continuesWithoutAuthenticationWhenAccessTokenIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/chat/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(cookieUtil.getCookieValue(request, CookieUtil.CookieType.ACCESS_TOKEN)).thenReturn(null);
        when(jwtProvider.validationToken(null)).thenReturn(TokenStatus.EMPTY);

        filter.doFilter(request, response, chain);

        verify(jwtProvider).validationToken(null);
        verify(jwtProvider, never()).parseToken(null);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void setsAuthenticationForValidAccessToken() throws ServletException, IOException {
        String accessToken = "access-token";
        Long userId = 1L;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/chat/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(cookieUtil.getCookieValue(request, CookieUtil.CookieType.ACCESS_TOKEN)).thenReturn(accessToken);
        when(jwtProvider.validationToken(accessToken)).thenReturn(TokenStatus.VALID);
        when(jwtProvider.parseToken(accessToken))
                .thenReturn(new TokenInfo(accessToken, TokenType.ACCESS, LocalDateTime.now().plusMinutes(10), userId));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userId);
    }

    @Test
    void returnsExpiredTokenErrorWhenAccessTokenIsExpired() throws ServletException, IOException {
        String expiredToken = "expired-token";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/chat/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(cookieUtil.getCookieValue(request, CookieUtil.CookieType.ACCESS_TOKEN)).thenReturn(expiredToken);
        when(jwtProvider.validationToken(expiredToken)).thenReturn(TokenStatus.EXPIRED);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(TokenErrorCode.EXPIRED_TOKEN.getStatusCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void returnsInvalidTokenErrorWhenAccessTokenIsInvalid() throws ServletException, IOException {
        String invalidToken = "invalid-token";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/chat/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(cookieUtil.getCookieValue(request, CookieUtil.CookieType.ACCESS_TOKEN)).thenReturn(invalidToken);
        when(jwtProvider.validationToken(invalidToken)).thenReturn(TokenStatus.INVALID);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(TokenErrorCode.INVALID_TOKEN.getStatusCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}