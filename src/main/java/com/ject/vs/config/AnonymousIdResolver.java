package com.ject.vs.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AnonymousIdResolver implements HandlerMethodArgumentResolver {

    private static final String COOKIE_NAME = "anonymous_id";
    private static final Duration MAX_AGE = Duration.ofDays(365);

    private final CookieProperties cookieProperties;

    @Override
    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AnonymousId.class)
                && parameter.getParameterType().equals(String.class);
    }

    @Override
    public String resolveArgument(org.springframework.core.MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest req = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse res = webRequest.getNativeResponse(HttpServletResponse.class);

        String existing = extractCookie(req, COOKIE_NAME);
        if (existing != null) return existing;

        String newId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, newId)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(MAX_AGE)
                .path("/")
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return newId;
    }

    private String extractCookie(HttpServletRequest req, String name) {
        if (req == null || req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
