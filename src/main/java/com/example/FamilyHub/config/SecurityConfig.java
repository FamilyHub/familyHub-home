package com.example.FamilyHub.config;

import com.example.FamilyHub.security.JwtAuthenticationFilter;
import com.example.FamilyHub.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.server.WebFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }

    @Bean
    public WebFilter jwtAuthenticationFilter(ServerSecurityContextRepository securityContextRepository) {
        return new JwtAuthenticationFilter(jwtTokenProvider, securityContextRepository);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, WebFilter jwtAuthenticationFilter) {
        logger.debug("Configuring security filter chain");
        
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> {
                    logger.debug("Configuring authorization rules");
                    exchanges
                        .pathMatchers("/ws/**", "/ws", "/api/auth/**").permitAll()
                        .anyExchange().authenticated();
                })
                .securityContextRepository(securityContextRepository())
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
} 