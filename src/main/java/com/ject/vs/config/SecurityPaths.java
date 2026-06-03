package com.ject.vs.config;

import java.util.ArrayList;
import java.util.List;

public class SecurityPaths {

    public static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/health/**",
            "/",
            "/error",
            "/ws/**",
            "/oauth2/authorization/**",
            "/login/oauth2/code/**",
            "/api/home/**"
    );

    /**
     * 로그인 없이도 접근 가능하지만, 토큰이 있으면 userId를 읽어야 하는 엔드포인트.
     * PUBLIC_ENDPOINTS(= JWT_EXCLUDED_PATHS)와 달리 JwtAuthFilter는 계속 실행되어
     * 토큰이 있으면 userId, 없으면 anonymousId로 동작한다.
     */
    public static final List<String> OPTIONAL_AUTH_ENDPOINTS = List.of(
            "/api/immersive-votes",
            "/api/immersive-votes/**",
            "/api/me/free-votes",
            "/api/votes/*",
            "/api/votes/*/participate",
            "/api/votes/*/result",
            "/api/votes/*/share",
            "/api/votes/*/emoji"
    );

    public static final List<String> JWT_EXCLUDED_PATHS = createJwtExcludedPaths();

    private static List<String> createJwtExcludedPaths() {
        List<String> paths = new ArrayList<>(PUBLIC_ENDPOINTS);

        paths.add("/auth/reissue");

        return List.copyOf(paths);
    }
}
