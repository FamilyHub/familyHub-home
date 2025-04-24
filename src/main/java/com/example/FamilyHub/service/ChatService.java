package com.example.FamilyHub.service;

import com.example.FamilyHub.dto.ChatMessageDTO;
import com.example.FamilyHub.dto.ChatHistoryResponse;
import com.example.FamilyHub.models.ChatMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatService {
    Mono<Void> sendMessage(ChatMessageDTO message);
    Mono<Void> deliverOfflineMessages(String userId);
    Mono<ChatMessage> getChatHistory(String senderId, String receiverId);
    Mono<Void> markMessageAsDelivered(String messageId);
    Mono<Void> markMessageAsRead(String messageId);
    Mono<Long> getUnreadMessageCount(String userId);
    Mono<Void> notifyTyping(String senderId, String receiverId);
    Flux<ChatMessage> getUnreadMessages(String senderId, String receiverId);

    /**
     * Get chat history between two users with cursor-based pagination
     * @param userId The ID of the current user
     * @param otherUserId The ID of the other user in the conversation
     * @param cursor The cursor for pagination (message ID)
     * @param limit Maximum number of messages to return
     * @return ChatHistoryResponse containing messages and pagination info
     */
    Mono<ChatHistoryResponse> getChatHistory(String userId, String otherUserId, String cursor, int limit);
} 