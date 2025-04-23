package com.example.FamilyHub.controller;

import com.example.FamilyHub.dto.ChatMessageDTO;
import com.example.FamilyHub.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Chat Controller
 * 
 * Handles real-time chat messages and notifications through WebSocket.
 * 
 * Endpoints:
 * - /ws: WebSocket connection endpoint
 * - /app/chat.send: Send a chat message
 * - /app/chat.typing: Send typing notification
 * 
 * Message Destinations:
 * - /user/{userId}/queue/messages: Private messages for specific user
 * - /user/{userId}/queue/typing: Typing notifications
 * - /user/{userId}/queue/status: Message status updates
 */
@RestController
@RequestMapping("/ws")
@RequiredArgsConstructor
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @GetMapping
    public Mono<Void> handleWebSocket(WebSocketSession session) {
        // Get user ID from session (you might need to implement this)
        String userId = getUserIdFromSession(session);
        sessions.put(userId, session);
        
        logger.debug("New WebSocket connection from user: {}", userId);

        return session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .flatMap(messageJson -> {
                try {
                    // Parse message
                    ChatMessageDTO messageDTO = objectMapper.readValue(messageJson, ChatMessageDTO.class);
                    logger.debug("Received WebSocket message: {}", messageDTO);
                    
                    // Process message
                    return chatService.sendMessage(messageDTO)
                        .flatMap(savedMessage -> {
                            try {
                                // Send confirmation back to sender
                                String responseJson = objectMapper.writeValueAsString(new MessageResponse("SENT", savedMessage));
                                return session.send(Mono.just(session.textMessage(responseJson)));
                            } catch (JsonProcessingException e) {
                                logger.error("Error serializing response: {}", e.getMessage());
                                return Mono.error(e);
                            }
                        });
                } catch (JsonProcessingException e) {
                    logger.error("Error parsing message: {}", e.getMessage());
                    try {
                        String errorJson = objectMapper.writeValueAsString(new MessageResponse("ERROR", e.getMessage()));
                        return session.send(Mono.just(session.textMessage(errorJson)));
                    } catch (JsonProcessingException ex) {
                        return Mono.error(ex);
                    }
                }
            })
            .then();
    }

    private String getUserIdFromSession(WebSocketSession session) {
        // Extract user ID from session attributes or headers
        // This depends on how you're handling authentication
        return session.getHandshakeInfo().getHeaders().getFirst("userId");
    }

    // Helper class for message responses
    private static class MessageResponse {
        private final String status;
        private final Object data;

        public MessageResponse(String status, Object data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() {
            return status;
        }

        public Object getData() {
            return data;
        }
    }
} 