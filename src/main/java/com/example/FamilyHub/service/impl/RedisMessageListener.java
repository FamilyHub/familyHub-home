package com.example.FamilyHub.service.impl;

import com.example.FamilyHub.models.ChatMessage;
import com.example.FamilyHub.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import jakarta.annotation.PostConstruct;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final Map<String, WebSocketSession> sessions;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        // Subscribe to chat channel
        redisTemplate.listenToChannel("chat")
            .subscribe(message -> {
                String messageId = message.getMessage();
                logger.debug("Received Redis notification for message: {}", messageId);
                
                // Fetch and send message
                chatMessageRepository.findById(messageId)
                    .subscribe(chatMessage -> {
                        WebSocketSession receiverSession = sessions.get(chatMessage.getReceiverId());
                        if (receiverSession != null) {
                            try {
                                String messageJson = objectMapper.writeValueAsString(chatMessage);
                                receiverSession.send(Mono.just(receiverSession.textMessage(messageJson)))
                                    .doOnSuccess(v -> logger.debug("Message sent via WebSocket to: {}", chatMessage.getReceiverId()))
                                    .doOnError(e -> logger.error("Error sending WebSocket message: {}", e.getMessage()))
                                    .subscribe();
                            } catch (Exception e) {
                                logger.error("Error serializing message: {}", e.getMessage());
                            }
                        }
                    });
            });
    }
} 