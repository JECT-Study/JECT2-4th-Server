package com.ject.vs.config;

import com.ject.vs.domain.TokenType;
import com.ject.vs.dto.TokenInfo;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final CookieUtil cookieUtil = mock(CookieUtil.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtProvider, cookieUtil);

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

        filter.doFilter(request, response, chain);

        verify(jwtProvider, never()).parseToken(null);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void setsAuthenticationForValidAccessToken() throws ServletException, IOException {
        String accessToken = "access-token";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/chat/messages");
        request.setCookies(new Cookie(CookieUtil.CookieType.ACCESS_TOKEN, accessToken));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(cookieUtil.getCookieValue(request, CookieUtil.CookieType.ACCESS_TOKEN)).thenReturn(accessToken);
        when(jwtProvider.validationToken(accessToken)).thenReturn(true);
        when(jwtProvider.parseToken(accessToken))
                .thenReturn(new TokenInfo(accessToken, TokenType.ACCESS, LocalDateTime.now().plusMinutes(10), 1L));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
    }
}
