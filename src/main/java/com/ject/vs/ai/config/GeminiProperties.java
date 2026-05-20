package com.ject.vs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String projectId,
        String location,
        String model,
        boolean enabled
) {
    public GeminiProperties {
        if (location == null || location.isBlank()) {
            location = "asia-northeast3";
        }
        if (model == null || model.isBlank()) {
            model = "gemini-1.5-flash";
        }
    }
}
