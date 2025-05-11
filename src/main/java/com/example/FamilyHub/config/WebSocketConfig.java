package com.example.FamilyHub.config;

import com.example.FamilyHub.dto.ChatMessageDTO;
import com.example.FamilyHub.service.ChatService;
import com.example.FamilyHub.service.impl.RedisMessageListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.FamilyHub.service.SessionManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private final RedisMessageListener redisMessageListener;
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Sinks.Many<ChatMessageDTO> messageSink = Sinks.many().multicast().onBackpressureBuffer();
    private final SessionManager sessionManager;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    public WebSocketConfig(ChatService chatService, RedisMessageListener redisMessageListener, SessionManager sessionManager) {
        this.chatService = chatService;
        this.redisMessageListener = redisMessageListener;
        this.sessionManager = sessionManager;
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
            // Extract userId from headers
//           String userId = session.getHandshakeInfo().getHeaders().getFirst("X-User-ID");

            String userId = null;
            String query = session.getHandshakeInfo().getUri().getQuery();
            if (query != null) {
                Map<String, String> queryParams = Arrays.stream(query.split("&"))
                        .map(param -> param.split("="))
                        .filter(param -> param.length == 2)
                        .collect(Collectors.toMap(param -> param[0], param -> param[1]));
                userId = queryParams.get("userId");
            }
            if (userId == null) {
                logger.error("No user ID found in WebSocket headers");
                return session.close();
            }
            
            // Add session to manager
            sessionManager.addSession(userId, session);
            
            // Deliver offline messages when user comes online
            chatService.deliverOfflineMessages(userId).subscribe();

            String finalUserId = userId;
            return session.receive()
                .doOnNext(message -> {




                    try {
                        String payload = message.getPayloadAsText();
                        ChatMessageDTO chatMessage = objectMapper.readValue(payload, ChatMessageDTO.class);

                        // Set sender ID from the session
                        chatMessage.setSenderId(finalUserId);
                        chatMessage.setTimestamp(java.time.LocalDateTime.now());
                        
                        // Save message to database
                        chatService.sendMessage(chatMessage).subscribe();
                    } catch (Exception e) {
                        logger.error("Error processing message: {}", e.getMessage());
                    }
                })
                .doFinally(signalType -> {
                    sessionManager.removeSession(finalUserId);
                })
                .then();
        };
    }
}



