package com.ject.vs.ai.config;

import com.google.cloud.vertexai.VertexAI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean
    @ConditionalOnProperty(name = "gemini.enabled", havingValue = "true")
    public VertexAI vertexAI(GeminiProperties properties) {
        if (properties.projectId() == null || properties.projectId().isBlank()) {
            log.warn("gemini.project-id is not configured. Vertex AI will not be initialized.");
            return null;
        }
        try {
            log.info("Initializing Vertex AI with project={}, location={}", properties.projectId(), properties.location());
            return new VertexAI(properties.projectId(), properties.location());
        } catch (Exception e) {
            log.error("Failed to initialize Vertex AI. AI insight will be disabled. Cause: {}", e.getMessage());
            return null;
        }
    }
}
