package com.example.FamilyHub.repository;

import com.example.FamilyHub.models.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
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

    /**
     * Find messages between two users with cursor-based pagination
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @param cursor Message ID to start from (exclusive)
     * @param pageable Pagination information
     * @return Flux of ChatMessage
     */
    @Query("{ $or: [ " +
           "{ senderId: ?0, receiverId: ?1 }, " +
           "{ senderId: ?1, receiverId: ?0 } " +
           "], " +
           "id: { $lt: ?2 } " +  // Using ID as cursor
           "}")
    Flux<ChatMessage> findByUsersAndCursor(
        String userId1,
        String userId2,
        String cursor,
        Pageable pageable
    );

    /**
     * Find messages between specific sender and receiver with cursor-based pagination
     * @param senderId The authenticated user's ID (from token)
     * @param receiverId The other user's ID (from request)
     * @param cursor Message ID to start from (exclusive)
     * @param pageable Pagination information
     * @return Flux of ChatMessage
     */
    @Query("{ " +
           "senderId: ?0, " +
           "receiverId: ?1, " +
           "id: { $lt: ?2 } " +  // Using ID as cursor
           "}")
    Flux<ChatMessage> findBySenderAndReceiver(
        String senderId,
        String receiverId,
        String cursor,
        Pageable pageable
    );
} 