package com.example.FamilyHub.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.LocalDateTime;

@Data
@Document(collection = "chat_messages")
/**
 * Compound Index for Chat Messages
 * 
 * This index optimizes queries for:
 * 1. Retrieving chat history between two users
 * 2. Sorting messages by timestamp
 * 
 * Index Structure:
 * - senderId: 1 (ascending) - First field for filtering by sender
 * - receiverId: 1 (ascending) - Second field for filtering by receiver
 * - timestamp: -1 (descending) - Third field for sorting messages by time
 * 
 * Benefits:
 * - Improves performance of chat history queries
 * - Enables efficient sorting of messages
 * - Supports pagination of chat messages
 * - Optimizes queries for unread message counts
 * 
 * Example Queries Optimized:
 * - Get chat history between two users
 * - Get latest messages for a conversation
 * - Get unread messages for a user
 * - Get message delivery status
 */
@CompoundIndex(name = "chat_message_idx", 
    def = "{'senderId': 1, 'receiverId': 1, 'timestamp': -1}")
public class ChatMessage {
    @Id
    private String id;
    
    @Indexed
    private String senderId;
    
    @Indexed
    private String receiverId;
    
    private String content;
    
    private MessageStatus status;
    
    private LocalDateTime timestamp;
    
    private LocalDateTime deliveredAt;
    
    private LocalDateTime readAt;
    
    public enum MessageStatus {
        SENT,
        DELIVERED,
        READ
    }
} 