package com.ject.vs.ai.config;

import com.google.cloud.vertexai.VertexAI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean
    @ConditionalOnProperty(name = "gemini.enabled", havingValue = "true")
    public VertexAI vertexAI(GeminiProperties properties) {
        return new VertexAI(properties.projectId(), properties.location());
    }
}
