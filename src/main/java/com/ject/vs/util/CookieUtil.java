package com.ject.vs.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    public String getCookieValue(HttpServletRequest request, String name) {
        if(request.getCookies() == null) {
            return null;
        }

        for(Cookie cookie : request.getCookies()) {     // 서버에 요청된 쿠키들을 확인해 refresh token인지 검증
            if(name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
