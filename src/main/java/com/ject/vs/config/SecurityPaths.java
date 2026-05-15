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
            "/login/oauth2/code/**"
    );

    public static final List<String> JWT_EXCLUDED_PATHS = createJwtExcludedPaths();

    private static List<String> createJwtExcludedPaths() {
        List<String> paths = new ArrayList<>(PUBLIC_ENDPOINTS);

        paths.add("/auth/reissue");

        return List.copyOf(paths);
    }
}
