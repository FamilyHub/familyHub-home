package com.example.FamilyHub.dto;

import com.example.FamilyHub.models.ChatMessage.MessageStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private MessageStatus status;
    private LocalDateTime timestamp;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
} 