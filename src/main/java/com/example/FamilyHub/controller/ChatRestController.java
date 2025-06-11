package com.example.FamilyHub.controller;

import com.example.FamilyHub.config.WebSocketConfig;
import com.example.FamilyHub.dto.ChatHistoryResponse;
import com.example.FamilyHub.dto.ChatMessageDTO;
import com.example.FamilyHub.dto.EnhancedChatHistoryResponse;
import com.example.FamilyHub.models.ChatMessage;
import com.example.FamilyHub.service.ChatService;
import com.example.FamilyHub.service.SessionManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.FamilyHub.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

/**
 * REST Chat Controller
 * 
 * Handles HTTP endpoints for chat-related operations.
 * 
 * Endpoints:
 * - POST /api/chat/send: Send a chat message
 * - GET /api/chat/history/{receiverId}: Get chat history with a user
 * - GET /api/chat/unread/count: Get count of unread messages
 * - GET /api/chat/unread/{senderId}: Get unread messages from a specific sender
 * - POST /api/chat/read/{messageId}: Mark a message as read
 * - POST /api/chat/delivered/{messageId}: Mark a message as delivered
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {
    private static final Logger logger = LoggerFactory.getLogger(ChatRestController.class);
    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebSocketConfig webSocketConfig;

    /**
     * Send a chat message
     * Path: POST /api/chat/send
     */
//    @PostMapping("/send")
//    public Mono<Void> sendMessage(
//            @RequestBody ChatMessageDTO messageDTO,
//            Authentication authentication) {
//        logger.debug("Received send message request from user: {}", authentication.getName());
//        String senderId = authentication.getName();
//        messageDTO.setSenderId(senderId);
//        messageDTO.setTimestamp(java.time.LocalDateTime.now());
//         chatService.sendMessage(messageDTO);
//    }

    /**
     * Get chat history with a specific user
     * Path: GET /api/chat/history/{receiverId}
     */
//    @GetMapping("/history/{receiverId}")
//    public Flux<ChatMessage> getChatHistory(
//            @PathVariable String receiverId,
//            Authentication authentication) {
//        String senderId = authentication.getName();
//        return chatService.getChatHistory(senderId, receiverId);
//    }

    /**
     * Get count of unread messages
     * Path: GET /api/chat/unread/count
     */
    @GetMapping("/unread/count")
    public Mono<ResponseEntity<Long>> getUnreadMessageCount(Authentication authentication) {
        String userId = authentication.getName();
        return chatService.getUnreadMessageCount(userId)
                .map(ResponseEntity::ok);
    }

    /**
     * Get unread messages from a specific sender
     * Path: GET /api/chat/unread/{senderId}
     */
    @GetMapping("/unread/{senderId}")
    public Flux<ChatMessage> getUnreadMessages(
            @PathVariable String senderId,
            Authentication authentication) {
        String receiverId = authentication.getName();
        return chatService.getUnreadMessages(senderId, receiverId);
    }

    /**
     * Mark a message as read
     * Path: POST /api/chat/read/{messageId}
     */
    @PostMapping("/read/{messageId}")
    public Mono<ResponseEntity<Void>> markMessageAsRead(
            @PathVariable String messageId,
            Authentication authentication) {
        String userId = authentication.getName();
        return chatService.markMessageAsRead(messageId)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    /**
     * Mark a message as delivered
     * Path: POST /api/chat/delivered/{messageId}
     */
    @PostMapping("/delivered/{messageId}")
    public Mono<ResponseEntity<Void>> markMessageAsDelivered(
            @PathVariable String messageId,
            Authentication authentication) {
        String userId = authentication.getName();
        return chatService.markMessageAsDelivered(messageId)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/history")
    public Mono<ResponseEntity<ChatHistoryResponse>> getChatHistory(
        @RequestParam String otherUserId,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "50") int limit,
        @RequestHeader("Authorization") String token
    ) {
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        if (userId == null) {
            logger.error("Invalid or missing token");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        
        logger.debug("Fetching chat history for user: {} with other user: {}", userId, otherUserId);
        return chatService.getChatHistory(userId, otherUserId, cursor, limit)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> {
                logger.error("Error fetching chat history: {}", e.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }

    /**
     * Get enhanced chat history with bidirectional pagination
     * 
     * @param receiverId The ID of the other user in the conversation
     * @param beforeTimestamp Messages before this timestamp (for older messages)
     * @param afterTimestamp Messages after this timestamp (for newer messages)
     * @param limit Maximum number of messages to return (default: 20)
     * @return EnhancedChatHistoryResponse containing messages and pagination info
     */
    @GetMapping("/enhanced-history/{receiverId}")
    public Mono<ResponseEntity<EnhancedChatHistoryResponse>> getEnhancedChatHistory(
        @PathVariable String receiverId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeTimestamp,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime afterTimestamp,
        @RequestParam(defaultValue = "20") int limit,
        @RequestHeader("Authorization") String token
    ) {
        String currentUserId = jwtTokenProvider.getUserIdFromToken(token);
        if (currentUserId == null) {
            logger.error("Invalid or missing token");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        
        // If no timestamps provided, we're doing an initial load
        if (beforeTimestamp == null && afterTimestamp == null) {
            // For initial load, get the most recent messages
            beforeTimestamp = LocalDateTime.now();
            afterTimestamp = LocalDateTime.now().minusDays(7); // Show messages from last 7 days by default
            
            return chatService.getEnhancedChatHistory(currentUserId, receiverId, beforeTimestamp, afterTimestamp, limit)
                .flatMap(response -> {
                    // If no messages found in the last 7 days, try to get any messages
                    if (response.getMessages().isEmpty()) {
                        // Use a reasonable minimum date (e.g., 1 year ago) instead of LocalDateTime.MIN
                        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
                        return chatService.getEnhancedChatHistory(
                            currentUserId, 
                            receiverId, 
                            LocalDateTime.now(), 
                            oneYearAgo, 
                            limit
                        );
                    }
                    return Mono.just(response);
                })
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    logger.error("Error getting enhanced chat history: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
        }
        
        // If timestamps are provided, use them as is
        if (beforeTimestamp == null) {
            beforeTimestamp = LocalDateTime.now();
        }
        if (afterTimestamp == null) {
            // Use a reasonable minimum date instead of LocalDateTime.MIN
            afterTimestamp = LocalDateTime.now().minusYears(1);
        }

        return chatService.getEnhancedChatHistory(currentUserId, receiverId, beforeTimestamp, afterTimestamp, limit)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> {
                logger.error("Error getting enhanced chat history: {}", e.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }

    @GetMapping("/sessions")
    public SessionManager connectedUsers() {
        return webSocketConfig.NoOfConnectedUsers();
    }
}