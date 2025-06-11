package com.example.FamilyHub.config;

import com.example.FamilyHub.security.JwtAuthenticationFilter;
import com.example.FamilyHub.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.server.WebFilter;

/**
 * Security Configuration Class
 * 
 * This class configures security settings for the Family Hub application.
 * It implements JWT-based authentication and authorization using Spring Security WebFlux.
 * 
 * Key Features:
 * - JWT-based authentication
 * - WebFlux security configuration
 * - CORS support
 * - Session management
 * - Protected endpoints
 * 
 * Security Rules:
 * - OPTIONS requests are permitted for all paths
 * - WebSocket endpoints (/ws/**) are public
 * - Authentication endpoints (/api/auth/**) are public
 * - All other endpoints require authentication
 * 
 * @author Family Hub Team
 * @version 1.0
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructs a new SecurityConfig with the required JWT token provider.
     * 
     * @param jwtTokenProvider The JWT token provider for authentication
     */
    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Creates a security context repository for storing authentication information.
     * Uses WebSession for storing security context.
     * 
     * @return Configured ServerSecurityContextRepository
     */
    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }

    /**
     * Creates a JWT authentication filter for processing JWT tokens.
     * 
     * @param securityContextRepository The repository for storing security context
     * @return Configured JWT authentication filter
     */
    @Bean
    public WebFilter jwtAuthenticationFilter(ServerSecurityContextRepository securityContextRepository) {
        return new JwtAuthenticationFilter(jwtTokenProvider, securityContextRepository);
    }

    /**
     * Configures the security filter chain for the application.
     * Sets up authentication, authorization, and security rules.
     * 
     * @param http The ServerHttpSecurity instance to configure
     * @param jwtAuthenticationFilter The JWT authentication filter
     * @return Configured SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, WebFilter jwtAuthenticationFilter) {
        logger.debug("Configuring security filter chain");
        
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> {
                    logger.debug("Configuring authorization rules");
                    exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Allow all OPTIONS requests
                        .pathMatchers("/ws/**", "/ws", "/api/auth/**").permitAll()
                        .anyExchange().authenticated();
                })
                .securityContextRepository(securityContextRepository())
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
} 