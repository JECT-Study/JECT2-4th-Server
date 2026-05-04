package com.ject.vs.config;

import com.ject.vs.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
<<<<<<< HEAD
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.ArrayList;
import java.util.List;
=======
import org.springframework.web.cors.CorsConfigurationSource;
>>>>>>> a1b87a6 (Allow trusted browser origins across environments)

@Configuration
public class SecurityConfig {
    @Bean
<<<<<<< HEAD
    public SecurityFilterChain securityFilterChain(HttpSecurity http, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, CustomOAuth2UserService customOAuth2UserService) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                )
=======
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                                                    CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
>>>>>>> a1b87a6 (Allow trusted browser origins across environments)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityPaths.PUBLIC_ENDPOINTS.toArray(String[]::new)).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/reissue").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)));

        return http.build();
    }
}
