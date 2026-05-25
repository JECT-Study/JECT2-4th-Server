package com.ject.vs.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void appliesLocalDevelopmentCorsDefaults() {
        CorsProperties properties = new CorsProperties(null, null, null, null, null, true, 0);
        CorsConfigurationSource source = new CorsConfig(properties).corsConfigurationSource();

        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).contains(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        );
        assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).contains("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin");
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getMaxAge()).isEqualTo(3600);
    }

    @Test
    void appliesConfiguredServerOrigins() {
        CorsProperties properties = new CorsProperties(
                List.of("https://app.example.com", "https://admin.example.com"),
                null,
                List.of("GET", "POST", "OPTIONS"),
                List.of("Authorization", "Content-Type"),
                List.of("Location"),
                true,
                7200
        );
        CorsConfigurationSource source = new CorsConfig(properties).corsConfigurationSource();

        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("https://app.example.com", "https://admin.example.com");
        assertThat(configuration.getAllowedMethods()).containsExactly("GET", "POST", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
        assertThat(configuration.getExposedHeaders()).containsExactly("Location");
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getMaxAge()).isEqualTo(7200);
    }
}
