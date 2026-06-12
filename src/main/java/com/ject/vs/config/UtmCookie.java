package com.ject.vs.config;

import com.ject.vs.user.domain.UtmAttribution;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 가입 유입 출처(UTM)를 비회원 브라우저에 박제하는 쿠키 헬퍼.
 *
 * <p>흐름: 프론트가 UTM 링크로 랜딩 → {@code /api/track/visit} 1회 호출 → 여기서 first-touch 쿠키 박제
 * → 이후 소셜 로그인 가입이 완료되는 순간 {@link com.ject.vs.config.OAuth2LoginSuccessHandler} 가
 * 쿠키를 읽어 users 테이블에 기록하고 쿠키를 만료시킨다.
 *
 * <p>값은 {@code s/m/c/ct} 키로 URL 인코딩해 한 쿠키에 담는다.
 */
@Component
@RequiredArgsConstructor
public class UtmCookie {

    static final String COOKIE_NAME = "utm_attribution";
    private static final Duration MAX_AGE = Duration.ofDays(30);

    private final CookieProperties cookieProperties;

    /** 최초 유입(first-touch)만 기록한다. 이미 쿠키가 있거나 UTM이 비어 있으면 아무것도 하지 않는다. */
    public void writeFirstTouch(HttpServletRequest req, HttpServletResponse res, UtmAttribution utm) {
        if (utm == null || utm.isEmpty()) {
            return;
        }
        if (extractCookie(req) != null) {
            return;
        }
        res.addHeader(HttpHeaders.SET_COOKIE, baseCookie(encode(utm), MAX_AGE).toString());
    }

    public UtmAttribution read(HttpServletRequest req) {
        String raw = extractCookie(req);
        return raw == null ? UtmAttribution.empty() : decode(raw);
    }

    /** 가입 시 출처를 소비한 뒤 쿠키를 만료시켜, 같은 브라우저의 다음 가입에 새 유입이 잡히도록 한다. */
    public void clear(HttpServletResponse res) {
        res.addHeader(HttpHeaders.SET_COOKIE, baseCookie("", Duration.ZERO).toString());
    }

    private ResponseCookie baseCookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(maxAge)
                .path("/")
                .build();
    }

    private String encode(UtmAttribution utm) {
        StringBuilder sb = new StringBuilder();
        append(sb, "s", utm.source());
        append(sb, "m", utm.medium());
        append(sb, "c", utm.campaign());
        append(sb, "ct", utm.content());
        return sb.toString();
    }

    private void append(StringBuilder sb, String key, String value) {
        if (value == null) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('&');
        }
        sb.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private UtmAttribution decode(String raw) {
        Map<String, String> map = new HashMap<>();
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            map.put(pair.substring(0, idx), URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
        }
        return UtmAttribution.of(map.get("s"), map.get("m"), map.get("c"), map.get("ct"));
    }

    private String extractCookie(HttpServletRequest req) {
        if (req == null || req.getCookies() == null) {
            return null;
        }
        return Arrays.stream(req.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
