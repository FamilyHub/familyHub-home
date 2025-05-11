package com.example.FamilyHub.repository;

import com.example.FamilyHub.models.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

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

    @Query("{ $or: [ " +
           "{ senderId: ?0, receiverId: ?1 }, " +
           "{ senderId: ?1, receiverId: ?0 } " +
           "], " +
           "timestamp: { $lt: ?2 } " +
           "}")
    Flux<ChatMessage> findByUsersAndTimestamp(
        String userId1,
        String userId2,
        LocalDateTime beforeTimestamp,
        Pageable pageable
    );

    /**
     * Find messages between two users with bidirectional pagination
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @param beforeTimestamp Messages before this timestamp (for older messages)
     * @param afterTimestamp Messages after this timestamp (for newer messages)
     * @param pageable Pagination information
     * @return Flux of ChatMessage
     */
    @Query("{ $or: [ " +
           "{ senderId: ?0, receiverId: ?1 }, " +
           "{ senderId: ?1, receiverId: ?0 } " +
           "], " +
           "$and: [ " +
           "{ timestamp: { $lt: ?2 } }, " +  // For older messages
           "{ timestamp: { $gt: ?3 } } " +   // For newer messages
           "] " +
           "}")
    Flux<ChatMessage> findByUsersAndTimestamps(
        String userId1,
        String userId2,
        LocalDateTime beforeTimestamp,
        LocalDateTime afterTimestamp,
        Pageable pageable
    );

    /**
     * Count total messages between two users
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return Mono of Long
     */
    @Query(value = "{ $or: [ " +
           "{ senderId: ?0, receiverId: ?1 }, " +
           "{ senderId: ?1, receiverId: ?0 } " +
           "]}", 
           count = true)
    Mono<Long> countByUsers(String userId1, String userId2);

    /**
     * Check if there are messages before a given timestamp
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @param timestamp The timestamp to check
     * @return Mono of Boolean
     */
    @Query(value = "{ $or: [ " +
           "{ senderId: ?0, receiverId: ?1 }, " +
           "{ senderId: ?1, receiverId: ?0 } " +
           "], " +
           "timestamp: { $lt: ?2 } }", 
           exists = true)
    Mono<Boolean> existsMessagesBefore(String userId1, String userId2, LocalDateTime timestamp);

    /**
     * Check if there are messages after a given timestamp
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @param timestamp The timestamp to check
     * @return Mono of Boolean
     */
    @Query(value = "{ $or: [ " +
           "{ senderId: ?0, receiverId: ?1 }, " +
           "{ senderId: ?1, receiverId: ?0 } " +
           "], " +
           "timestamp: { $gt: ?2 } }", 
           exists = true)
    Mono<Boolean> existsMessagesAfter(String userId1, String userId2, LocalDateTime timestamp);
} 