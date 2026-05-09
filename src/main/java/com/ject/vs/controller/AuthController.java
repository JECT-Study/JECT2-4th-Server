package com.ject.vs.controller;

import com.ject.vs.dto.TokenInfo;
import com.ject.vs.dto.TokenReissueResponse;
import com.ject.vs.service.AuthService;
import com.ject.vs.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @Value("${app.jwt.access-token-expiration-seconds}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration-seconds}")
    private long refreshTokenExpiration;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @PostMapping("/auth/reissue")
    public ResponseEntity<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.getCookieValue(request, CookieUtil.CookieType.REFRESH_TOKEN);

        TokenReissueResponse tokenResponse = authService.reissueAccessToken(refreshToken);

        ResponseCookie accessTokenCookie = ResponseCookie.from(
                CookieUtil.CookieType.ACCESS_TOKEN,
                tokenResponse.accessToken()
        )
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("None")
                .maxAge(accessTokenExpiration)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(
                CookieUtil.CookieType.REFRESH_TOKEN,
                tokenResponse.refreshToken()
        )
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("None")
                .maxAge(refreshTokenExpiration)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok().build();
    }
}
