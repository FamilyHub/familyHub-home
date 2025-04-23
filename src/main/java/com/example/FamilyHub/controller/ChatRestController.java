package com.example.FamilyHub.controller;

import com.example.FamilyHub.dto.ChatMessageDTO;
import com.example.FamilyHub.models.ChatMessage;
import com.example.FamilyHub.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    /**
     * Send a chat message
     * Path: POST /api/chat/send
     */
    @PostMapping("/send")
    public Mono<ResponseEntity<ChatMessage>> sendMessage(
            @RequestBody ChatMessageDTO messageDTO,
            Authentication authentication) {
        logger.debug("Received send message request from user: {}", authentication.getName());
        String senderId = authentication.getName();
        messageDTO.setSenderId(senderId);
        messageDTO.setTimestamp(java.time.LocalDateTime.now());
        return chatService.sendMessage(messageDTO)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> logger.debug("Message sent successfully"))
                .doOnError(error -> logger.error("Error sending message: {}", error.getMessage()));
    }

    /**
     * Get chat history with a specific user
     * Path: GET /api/chat/history/{receiverId}
     */
    @GetMapping("/history/{receiverId}")
    public Flux<ChatMessage> getChatHistory(
            @PathVariable String receiverId,
            Authentication authentication) {
        String senderId = authentication.getName();
        return chatService.getChatHistory(senderId, receiverId);
    }

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
} 