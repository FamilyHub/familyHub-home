package com.example.FamilyHub.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

public class JwtAuthenticationFilter implements WebFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh-token",
            "/ws",
            "/ws/info"
    );

    private final JwtTokenProvider tokenProvider;
    private final ServerSecurityContextRepository securityContextRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, ServerSecurityContextRepository securityContextRepository) {
        this.tokenProvider = tokenProvider;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Skip JWT validation for public paths
//        if (isPublicPath(path)) {
//            logger.debug("Skipping JWT validation for public path: {}", path);
//            return chain.filter(exchange);
//        }

        String token = extractToken(exchange);
        if (token == null) {
            logger.warn("No JWT token found in request");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            if (tokenProvider.validateToken(token)) {
                logger.debug("Valid JWT token found");
                Authentication authentication = tokenProvider.getAuthentication(token);
                SecurityContext securityContext = new SecurityContextImpl(authentication);
                
                return securityContextRepository.save(exchange, securityContext)
                    .then(chain.filter(exchange));
            } else {
                logger.warn("Invalid JWT token");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        } catch (ExpiredJwtException e) {
            logger.warn("Expired JWT token: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("X-Error-Message", "Token has expired");
            return exchange.getResponse().setComplete();
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("X-Error-Message", "Unsupported token format");
            return exchange.getResponse().setComplete();
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("X-Error-Message", "Malformed token");
            return exchange.getResponse().setComplete();
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("X-Error-Message", "Invalid token signature");
            return exchange.getResponse().setComplete();
        } catch (Exception e) {
            logger.error("Unexpected error during JWT validation: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            exchange.getResponse().getHeaders().add("X-Error-Message", "Internal server error");
            return exchange.getResponse().setComplete();
        }
    }

    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
} 