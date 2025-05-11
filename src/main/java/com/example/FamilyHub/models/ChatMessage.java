package com.example.FamilyHub.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.LocalDateTime;

@Data
@Document(collection = "chat_messages")
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