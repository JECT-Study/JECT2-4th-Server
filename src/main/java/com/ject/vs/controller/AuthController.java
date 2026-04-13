package com.ject.vs.controller;

import com.ject.vs.dto.LoginTokenResponse;
import com.ject.vs.dto.OAuthAttributes;
import com.ject.vs.dto.TokenInfo;
import com.ject.vs.service.AuthService;
import com.ject.vs.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @PostMapping("/auth/reissue")
    public ResponseEntity<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.getCookieValue(request, "refresh_token");

        TokenInfo newAccessTokenInfo = authService.reissueAccessToken(refreshToken);

        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", newAccessTokenInfo.getTokenValue())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("None")
                .maxAge(60 * 30)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        return ResponseEntity.ok().build();
    }
}
