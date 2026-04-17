package com.ject.vs.controller;

import com.ject.vs.dto.TokenInfo;
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

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @PostMapping("/auth/reissue")
    public ResponseEntity<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.getCookieValue(request, "refresh_token");

        TokenInfo newAccessTokenInfo = authService.reissueAccessToken(refreshToken);

        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", newAccessTokenInfo.tokenValue())
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
