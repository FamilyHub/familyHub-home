package com.example.FamilyHub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Configuration Class
 * 
 * This class configures Redis for the Family Hub application, providing
 * reactive Redis template for pub/sub messaging and caching.
 * 
 * Key Features:
 * - Configures Redis connection
 * - Sets up serialization
 * - Provides reactive Redis template
 * - Manages Redis operations
 * 
 * Usage:
 * - Pub/sub messaging for real-time updates
 * - Caching frequently accessed data
 * - Session management
 * - Message queue management
 * 
 * Serialization:
 * - Uses Jackson for JSON serialization
 * - String serialization for keys
 * - Custom serialization context
 * 
 * @author Family Hub Team
 * @version 1.0
 */
@Configuration
public class RedisConfig {

    /**
     * Creates and configures a ReactiveRedisTemplate for Redis operations.
     * 
     * This method:
     * 1. Sets up Jackson JSON serializer
     * 2. Configures serialization context
     * 3. Creates reactive Redis template
     * 
     * @param connectionFactory The Redis connection factory
     * @return Configured ReactiveRedisTemplate
     */
    @Bean
    @Primary
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<String> serializer = new Jackson2JsonRedisSerializer<>(String.class);
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, String> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());
        
        RedisSerializationContext<String, String> context = builder
                .value(serializer)
                .build();
        
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
} 