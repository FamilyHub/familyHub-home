package com.example.FamilyHub.repository;

import com.example.FamilyHub.models.ChatMessage;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatMessageRepository extends ReactiveMongoRepository<ChatMessage, String> {
    Flux<ChatMessage> findBySenderIdAndReceiverIdOrReceiverIdAndSenderId(
            String senderId1, String receiverId1, String senderId2, String receiverId2
    );
    
    Mono<Long> countByReceiverIdAndStatus(String receiverId, ChatMessage.MessageStatus status);
    
    Flux<ChatMessage> findBySenderIdAndReceiverIdAndStatus(
            String senderId, String receiverId, ChatMessage.MessageStatus status
    );
} 