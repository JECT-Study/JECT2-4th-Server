package com.ject.vs.config;

import com.ject.vs.dto.LoginTokenResponse;
import com.ject.vs.dto.OAuthAttributes;
import com.ject.vs.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.oauth2.redirect-success-url}")
    private String redirectSuccessUrl;

    @Value("${app.cookie.secure:true")      // 운영 상황에서는 true로 변경 https 사용할 경우
    private boolean secureCookie;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        String userNameAttributeName = "sub";

        OAuthAttributes attributes = OAuthAttributes.of(
                registrationId,
                userNameAttributeName,
                oauth2User.getAttributes()
        );

        LoginTokenResponse tokenResponse = authService.socialLogin(attributes.getSub());

        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", tokenResponse.getAccessToken())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("None")
                .maxAge(60 * 30)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", tokenResponse.getRefreshToken())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("None")
                .maxAge(60 * 60 * 24 * 14)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        getRedirectStrategy().sendRedirect(request, response, redirectSuccessUrl);
    }
}