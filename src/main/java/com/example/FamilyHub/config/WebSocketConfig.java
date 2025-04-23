package com.example.FamilyHub.config;

import com.example.FamilyHub.dto.ChatMessageDTO;
import com.example.FamilyHub.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Configuration Class
 * 
 * This class configures the WebSocket message broker and endpoints
 * for the Family Hub application.
 * 
 * Key Features:
 * - Enables WebSocket message broker
 * - Configures message routing
 * - Sets up STOMP endpoints
 * - Manages message prefixes
 * 
 * Usage:
 * - Real-time messaging
 * - Chat functionality
 * - Notifications
 * 
 * Endpoints:
 * - /ws: WebSocket endpoint
 * - /app: Application prefix
 * - /topic: Topic prefix
 * - /queue: Queue prefix
 * - /user: User-specific prefix
 * 
 * @author Family Hub Team
 * @version 1.0
 */
@Configuration
public class WebSocketConfig implements WebFluxConfigurer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatService chatService;
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Sinks.Many<ChatMessageDTO> messageSink = Sinks.many().multicast().onBackpressureBuffer();

    public WebSocketConfig(ChatService chatService) {
        this.chatService = chatService;
    }

    @Bean
    public HandlerMapping handlerMapping() {

        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws", webSocketHandler());

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    public WebSocketHandler webSocketHandler() {
        return session -> {
            // Extract user ID from the session (you might need to adjust this based on your auth setup)
            String userId = extractUserIdFromSession(session);
            if (userId != null) {
                userSessions.put(userId, session);
                System.out.println("User connected: " + userId);
            }

            return session.receive()
                .doOnNext(message -> {
                    try {
                        String payload = message.getPayloadAsText();
                        ChatMessageDTO chatMessage = objectMapper.readValue(payload, ChatMessageDTO.class);
                        
                        // Set sender ID from the session
                        chatMessage.setSenderId(userId);
                        chatMessage.setTimestamp(java.time.LocalDateTime.now());
                        
                        // Save message to database
                        chatService.sendMessage(chatMessage).subscribe();
                        
                        // Send to recipient if online
                        WebSocketSession recipientSession = userSessions.get(chatMessage.getReceiverId());
                        if (recipientSession != null) {
                            recipientSession.send(Mono.just(recipientSession.textMessage(payload))).subscribe();
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing message: " + e.getMessage());
                    }
                })
                .doFinally(signalType -> {
                    if (userId != null) {
                        userSessions.remove(userId);
                        System.out.println("User disconnected: " + userId);
                    }
                })
                .then();
        };
    }

    private String extractUserIdFromSession(WebSocketSession session) {
        // Extract user ID from the session attributes or headers
        // This is just an example - adjust based on your auth setup
        return session.getHandshakeInfo().getHeaders().getFirst("userId");
    }
} 