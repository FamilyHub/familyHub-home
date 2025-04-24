package com.example.FamilyHub.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatHistoryResponse {
    private List<ChatMessageDTO> messages;
    private String nextCursor;
    private boolean hasMore;
} 