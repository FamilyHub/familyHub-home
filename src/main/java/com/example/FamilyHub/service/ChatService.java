package com.example.FamilyHub.service;

import com.example.FamilyHub.dto.ChatMessageDTO;
import com.example.FamilyHub.models.ChatMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatService {
    Mono<ChatMessage> sendMessage(ChatMessageDTO messageDTO);
    Flux<ChatMessage> getChatHistory(String senderId, String receiverId);
    Mono<Void> markMessageAsDelivered(String messageId);
    Mono<Void> markMessageAsRead(String messageId);
    Mono<Long> getUnreadMessageCount(String userId);
    Mono<Void> notifyTyping(String senderId, String receiverId);
    Flux<ChatMessage> getUnreadMessages(String senderId, String receiverId);
} 