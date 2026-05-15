package com.ject.vs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
        boolean secure,
        String sameSite
) {
    public CookieProperties {
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "None";
        }
    }
}
