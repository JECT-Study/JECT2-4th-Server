package com.ject.vs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedOriginPatterns,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        boolean allowCredentials,
        long maxAge
) {
    public CorsProperties {
        allowedOrigins = defaultsIfEmpty(allowedOrigins, List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173"
        ));
        allowedOriginPatterns = defaultsIfEmpty(allowedOriginPatterns, List.of());
        allowedMethods = defaultsIfEmpty(allowedMethods, List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        allowedHeaders = defaultsIfEmpty(allowedHeaders, List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        exposedHeaders = defaultsIfEmpty(exposedHeaders, List.of());
        maxAge = maxAge > 0 ? maxAge : 3600;
    }

    private static List<String> defaultsIfEmpty(List<String> values, List<String> defaults) {
        if (values == null || values.isEmpty()) {
            return List.copyOf(defaults);
        }
        return List.copyOf(values);
    }
}
