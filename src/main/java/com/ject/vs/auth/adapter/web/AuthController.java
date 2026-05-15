package com.ject.vs.auth.adapter.web;

import com.ject.vs.auth.port.AuthService;
import com.ject.vs.auth.port.in.dto.TokenReissueResponse;
import com.ject.vs.config.CookieProperties;
import com.ject.vs.config.JwtProperties;
import com.ject.vs.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    @PostMapping("/auth/reissue")
    public ResponseEntity<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.getCookieValue(request, CookieUtil.CookieType.REFRESH_TOKEN);

        TokenReissueResponse tokenResponse = authService.reissueAccessToken(refreshToken);

        long accessTokenExpiration = jwtProperties.accessTokenExpirationSeconds();
        long refreshTokenExpiration = jwtProperties.refreshTokenExpirationSeconds();

        ResponseCookie accessTokenCookie = ResponseCookie.from(
                CookieUtil.CookieType.ACCESS_TOKEN,
                tokenResponse.accessToken()
        )
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .sameSite(cookieProperties.sameSite())
                .maxAge(accessTokenExpiration)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(
                CookieUtil.CookieType.REFRESH_TOKEN,
                tokenResponse.refreshToken()
        )
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .sameSite(cookieProperties.sameSite())
                .maxAge(refreshTokenExpiration)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok().build();
    }
}
