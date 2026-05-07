package com.ject.vs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.domain.TokenStatus;
import com.ject.vs.dto.ErrorResponse;
import com.ject.vs.exception.ErrorCode;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final List<String> JWT_EXCLUDED_PATHS = List.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/**",
            "/actuator/health",
            "/actuator/health/**",
            "/",
            "/error",
            "/auth/reissue"
    );

    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = getRequestPath(request);
        return JWT_EXCLUDED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String getRequestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (StringUtils.hasText(servletPath)) {
            return servletPath;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 토큰 확인
        String accessToken = cookieUtil.getCookieValue(request, CookieUtil.CookieType.ACCESS_TOKEN);

        TokenStatus status = jwtProvider.validationToken(accessToken);

        switch (status) {
            case VALID -> {
                setAuthentication(request, accessToken);
                filterChain.doFilter(request, response);
            }
            case EMPTY -> filterChain.doFilter(request, response);
            case EXPIRED -> sendErrorResponse(response, ErrorCode.EXPIRED_TOKEN);
            case INVALID -> sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
        }
    }

    private void setAuthentication(HttpServletRequest request, String token) {
        var tokenInfo = jwtProvider.parseToken(token);

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                tokenInfo.userId(),
                null,
                AuthorityUtils.NO_AUTHORITIES
        );

        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
