package com.example.FamilyHub.dto;

import lombok.Data;
import java.util.List;

@Data
public class EnhancedChatHistoryResponse {
    private List<ChatMessageDTO> messages;
    private String nextCursor;      // For loading older messages
    private String previousCursor;  // For loading newer messages
    private boolean hasMore;        // Whether there are more older messages
    private boolean hasPrevious;    // Whether there are more newer messages
    private long totalMessages;     // Total count of messages in the conversation
} 