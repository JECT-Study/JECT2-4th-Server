package com.ject.vs.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestPropertiesConfig {

    @Bean
    public JwtProperties jwtProperties() {
        // test jwt secret (base64 of 32+ bytes)
        String testSecret = "dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItSFM1MTI=";
        return new JwtProperties(testSecret, 3600, 1209600);
    }

    @Bean
    public CookieProperties cookieProperties() {
        return new CookieProperties(false, "None");
    }

    @Bean
    public OAuth2Properties oauth2Properties() {
        return new OAuth2Properties(
                "http://localhost:3000/oauth2/redirect",
                "http://localhost:3000/extra-info"
        );
    }
}