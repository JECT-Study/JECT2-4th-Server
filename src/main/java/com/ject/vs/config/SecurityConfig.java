package com.ject.vs.config;

import com.ject.vs.auth.port.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            CustomOAuth2UserService customOAuth2UserService,
            JwtAuthFilter jwtAuthFilter,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers("/api/**", "/auth/reissue")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityPaths.PUBLIC_ENDPOINTS.toArray(String[]::new)).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/reissue").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/nickname/suggest", "/api/users/imagecolor/suggest").permitAll()
                        .requestMatchers(SecurityPaths.OPTIONAL_AUTH_ENDPOINTS.toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":\"AUTH_001\",\"message\":\"인증이 필요합니다.\"}");
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
