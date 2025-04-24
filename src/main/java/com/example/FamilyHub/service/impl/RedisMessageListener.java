package com.example.FamilyHub.service.impl;

import com.example.FamilyHub.dto.ChatMessageDTO;
import com.example.FamilyHub.service.SessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Service
public class RedisMessageListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);
    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;

    public RedisMessageListener(ObjectMapper objectMapper, SessionManager sessionManager) {
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String messageBody = new String(message.getBody());
            logger.debug("Received message from Redis: {}", messageBody);
            
            ChatMessageDTO chatMessage = objectMapper.readValue(messageBody, ChatMessageDTO.class);
            String recipientId = chatMessage.getReceiverId();
            
            WebSocketSession session = sessionManager.getSession(recipientId);
            if (session != null && session.isOpen()) {
                logger.debug("Sending message to online user: {}", recipientId);
                try {
                    String messageJson = objectMapper.writeValueAsString(chatMessage);
                    Mono.just(chatMessage)
                        .flatMap(msg -> session.send(Mono.just(session.textMessage(messageJson))))
                        .doOnSuccess(v -> logger.debug("Message sent successfully to user: {}", recipientId))
                        .doOnError(e -> logger.error("Error sending message to user {}: {}", recipientId, e.getMessage()))
                        .subscribe();
                } catch (JsonProcessingException e) {
                    logger.error("Error serializing message for user {}: {}", recipientId, e.getMessage());
                }
            } else {
                logger.debug("Recipient {} is not online, message will be delivered when they come online", recipientId);
            }
        } catch (IOException e) {
            logger.error("Error processing Redis message: {}", e.getMessage());
        }
    }

    public Mono<Void> deliverOfflineMessages(String userId) {
        // Implementation for delivering offline messages
        return Mono.empty();
    }
} 