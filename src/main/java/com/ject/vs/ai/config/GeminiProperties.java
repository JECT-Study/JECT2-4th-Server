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
            // asia-northeast3(서울)은 Gemini 모델 가용성이 없어 NOT_FOUND가 발생한다.
            // Gemini 모델이 보장되는 리전을 기본값으로 사용한다. (필요 시 GEMINI_LOCATION으로 override)
            location = "us-central1";
        }
        if (model == null || model.isBlank()) {
            // gemini-1.5 retired, 2.0-flash는 프로젝트 접근권 없음(404). 접근 확인된 모델 사용. (필요 시 GEMINI_MODEL로 override)
            model = "gemini-2.5-flash";
        }
    }
}
