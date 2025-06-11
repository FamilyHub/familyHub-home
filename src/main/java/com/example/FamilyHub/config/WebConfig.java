package com.example.FamilyHub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * Web Configuration Class
 * 
 * This class configures CORS (Cross-Origin Resource Sharing) settings for the Family Hub application.
 * It enables secure cross-origin requests and responses between the frontend and backend.
 * 
 * Key Features:
 * - Configures allowed origins (currently localhost:3000)
 * - Sets up allowed HTTP methods
 * - Defines allowed headers
 * - Manages exposed headers
 * - Configures credentials handling
 * - Sets cache duration
 * 
 * Security Considerations:
 * - Only allows specific origins
 * - Restricts HTTP methods
 * - Controls header exposure
 * - Manages credentials carefully
 * 
 * @author Family Hub Team
 * @version 1.0
 */
@Configuration
public class WebConfig {

    /**
     * Creates and configures a CORS filter for the application.
     * 
     * This method:
     * 1. Sets up allowed origins (currently only localhost:3000)
     * 2. Configures allowed HTTP methods (GET, POST, PUT, DELETE, OPTIONS)
     * 3. Defines allowed headers for cross-origin requests
     * 4. Specifies exposed headers in responses
     * 5. Enables credentials
     * 6. Sets cache duration to 1 hour
     * 
     * @return Configured CorsWebFilter instance
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Accept-Language",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Headers",
            "Access-Control-Request-Method",
            "Connection",
            "Sec-Fetch-Dest",
            "Sec-Fetch-Mode",
            "Sec-Fetch-Site",
            "User-Agent",
            "Referer",
            "sec-ch-ua",
            "sec-ch-ua-mobile",
            "sec-ch-ua-platform"
        ));
        corsConfig.setExposedHeaders(Arrays.asList("Authorization"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}

