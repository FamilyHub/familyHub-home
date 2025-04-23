package com.example.FamilyHub.service.impl;

import com.example.FamilyHub.dto.ChatMessageDTO;
import com.example.FamilyHub.models.ChatMessage;
import com.example.FamilyHub.repository.ChatMessageRepository;
import com.example.FamilyHub.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatMessageRepository chatMessageRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Mono<ChatMessage> sendMessage(ChatMessageDTO messageDTO) {
        logger.debug("Processing message: {}", messageDTO);
        
        // Create and save message
        ChatMessage message = new ChatMessage();
        message.setSenderId(messageDTO.getSenderId());
        message.setReceiverId(messageDTO.getReceiverId());
        message.setContent(messageDTO.getContent());
        message.setStatus(ChatMessage.MessageStatus.SENT);
        message.setTimestamp(LocalDateTime.now());

        return chatMessageRepository.save(message)
            .doOnSuccess(savedMessage -> {
                logger.debug("Message saved to MongoDB: {}", savedMessage);
                
                // Publish to Redis
                redisTemplate.convertAndSend("chat", savedMessage.getId())
                    .doOnSuccess(v -> logger.debug("Message published to Redis: {}", savedMessage.getId()))
                    .subscribe();

                // Send via WebSocket if receiver is connected
                WebSocketSession receiverSession = sessions.get(savedMessage.getReceiverId());
                if (receiverSession != null) {
                    try {
                        String messageJson = objectMapper.writeValueAsString(savedMessage);
                        receiverSession.send(Mono.just(receiverSession.textMessage(messageJson)))
                            .doOnSuccess(v -> logger.debug("Message sent via WebSocket to: {}", savedMessage.getReceiverId()))
                            .doOnError(e -> logger.error("Error sending WebSocket message: {}", e.getMessage()))
                            .subscribe();
                    } catch (Exception e) {
                        logger.error("Error serializing message: {}", e.getMessage());
                    }
                }
            })
            .doOnError(e -> logger.error("Error processing message: {}", e.getMessage()));
    }

    @Override
    public Flux<ChatMessage> getChatHistory(String senderId, String receiverId) {
        logger.debug("Getting chat history between {} and {}", senderId, receiverId);
        return chatMessageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderId(
            senderId, receiverId, senderId, receiverId
        )
        .doOnNext(message -> logger.debug("Found message: {}", message))
        .sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));
    }

    @Override
    public Mono<Long> getUnreadMessageCount(String userId) {
        logger.debug("Getting unread message count for user: {}", userId);
        return chatMessageRepository.countByReceiverIdAndStatus(userId, ChatMessage.MessageStatus.SENT)
            .doOnSuccess(count -> logger.debug("Unread message count: {}", count));
    }

    @Override
    public Flux<ChatMessage> getUnreadMessages(String senderId, String receiverId) {
        logger.debug("Getting unread messages from {} to {}", senderId, receiverId);
        return chatMessageRepository.findBySenderIdAndReceiverIdAndStatus(
            senderId, receiverId, ChatMessage.MessageStatus.SENT
        )
        .doOnNext(message -> logger.debug("Found unread message: {}", message));
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageId) {
        logger.debug("Marking message as read: {}", messageId);
        return chatMessageRepository.findById(messageId)
            .flatMap(message -> {
                message.setStatus(ChatMessage.MessageStatus.READ);
                message.setReadAt(LocalDateTime.now());
                return chatMessageRepository.save(message)
                    .doOnSuccess(m -> logger.debug("Message marked as read: {}", m));
            })
            .then();
    }

    @Override
    public Mono<Void> markMessageAsDelivered(String messageId) {
        logger.debug("Marking message as delivered: {}", messageId);
        return chatMessageRepository.findById(messageId)
            .flatMap(message -> {
                message.setStatus(ChatMessage.MessageStatus.DELIVERED);
                message.setDeliveredAt(LocalDateTime.now());
                return chatMessageRepository.save(message)
                    .doOnSuccess(m -> logger.debug("Message marked as delivered: {}", m));
            })
            .then();
    }

    @Override
    public Mono<Void> notifyTyping(String senderId, String receiverId) {
        WebSocketSession receiverSession = sessions.get(receiverId);
        if (receiverSession != null) {
            return receiverSession.send(Mono.just(receiverSession.textMessage(
                Map.of("senderId", senderId, "typing", true).toString()
            )));
        }
        return Mono.empty();
    }

    public void addSession(String userId, WebSocketSession session) {
        sessions.put(userId, session);
        logger.debug("Added WebSocket session for user: {}", userId);
    }

    public void removeSession(String userId) {
        sessions.remove(userId);
        logger.debug("Removed WebSocket session for user: {}", userId);
    }
} 