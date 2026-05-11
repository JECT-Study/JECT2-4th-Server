package com.ject.vs.config;

import com.ject.vs.domain.UserStatus;
import com.ject.vs.dto.LoginTokenResponse;
import com.ject.vs.dto.OAuthAttributes;
import com.ject.vs.exception.CustomException;
import com.ject.vs.service.AuthService;
import com.ject.vs.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.oauth2.redirect-success-url}")
    private String homeUrl;

    @Value("${app.oauth2.extra-info-url}")
    private String extraInfoUrl;

    @Value("${app.jwt.access-token-expiration-seconds}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration-seconds}")
    private long refreshTokenExpiration;

    @Value("${app.cookie.secure:false}")      // 운영 상황에서는 true로 변경 https 사용할 경우
    private boolean secureCookie;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        try {
            LoginTokenResponse loginResponse = authService.socialLogin(email);

            addTokenCookies(response, loginResponse);

            // 상태(REGISTER, UNREGISTER)에 따른 리다이렉트 경로 결정
            String targetUrl = determineTargetUrl(loginResponse.getUserStatus());

            // 리다이렉트 실행
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (CustomException e) {
            // 401 또는 500 에러 처리 (로그인 실패 시)
            response.sendError(e.getErrorCode().getHttpStatus().value(), e.getMessage());
        }
    }

    private void addTokenCookies(HttpServletResponse response, LoginTokenResponse loginResponse) {
        // 30분
        ResponseCookie accessTokenCookie = ResponseCookie.from(CookieUtil.CookieType.ACCESS_TOKEN, loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(accessTokenExpiration)
                .sameSite("Lax")
                .build();

        // 30일
        ResponseCookie refreshTokenCookie = ResponseCookie.from(CookieUtil.CookieType.REFRESH_TOKEN, loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    private String determineTargetUrl(UserStatus status) {
        if(UserStatus.REGISTER.equals(status)) {
            return homeUrl;
        }
        return extraInfoUrl;
    }
}