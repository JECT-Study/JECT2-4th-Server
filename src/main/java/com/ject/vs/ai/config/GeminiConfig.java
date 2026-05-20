package com.ject.vs.ai.config;

import com.google.cloud.vertexai.VertexAI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean
    public VertexAI vertexAI(GeminiProperties properties) {
        if (!properties.enabled() || properties.projectId() == null || properties.projectId().isBlank()) {
            return null;
        }
        return new VertexAI(properties.projectId(), properties.location());
    }
}
