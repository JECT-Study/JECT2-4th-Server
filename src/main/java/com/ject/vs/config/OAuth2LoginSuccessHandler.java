package com.ject.vs.config;

import com.ject.vs.auth.port.AuthService;
import com.ject.vs.auth.port.in.dto.LoginTokenResponse;
import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.user.domain.UserStatus;
import com.ject.vs.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
    private final OAuth2Properties oauth2Properties;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        try {
            LoginTokenResponse loginResponse = authService.socialLogin(email);

            addTokenCookies(response, loginResponse);

            String targetUrl = determineTargetUrl(loginResponse.getUserStatus());

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (BusinessException e) {
            response.sendError(e.getErrorCode().getStatusCode(), e.getMessage());
        }
    }

    private void addTokenCookies(HttpServletResponse response, LoginTokenResponse loginResponse) {
        long accessTokenExpiration = jwtProperties.accessTokenExpirationSeconds();
        long refreshTokenExpiration = jwtProperties.refreshTokenExpirationSeconds();

        ResponseCookie accessTokenCookie = ResponseCookie.from(CookieUtil.CookieType.ACCESS_TOKEN, loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .maxAge(accessTokenExpiration)
                .sameSite(cookieProperties.sameSite())
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(CookieUtil.CookieType.REFRESH_TOKEN, loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .maxAge(refreshTokenExpiration)
                .sameSite(cookieProperties.sameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    private String determineTargetUrl(UserStatus status) {
        if (UserStatus.REGISTER.equals(status)) {
            return oauth2Properties.redirectSuccessUrl();
        }
        return oauth2Properties.extraInfoUrl();
    }
}
