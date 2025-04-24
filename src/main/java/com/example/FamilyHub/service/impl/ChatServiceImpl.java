package com.example.FamilyHub.service.impl;

import com.example.FamilyHub.dto.ChatMessageDTO;
import com.example.FamilyHub.models.ChatMessage;
import com.example.FamilyHub.repository.ChatMessageRepository;
import com.example.FamilyHub.service.ChatService;
import com.example.FamilyHub.service.SessionManager;
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
    private static final String OFFLINE_MESSAGES_KEY = "offline:messages:";
    private static final String OFFLINE_MESSAGE_PATTERN = "offline:messages:*";

    private final ChatMessageRepository chatMessageRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final SessionManager sessionManager;

    @Override
    public Mono<Void> sendMessage(ChatMessageDTO messageDTO) {
        logger.debug("Processing message: {}", messageDTO);
        
        // Create and save message
        ChatMessage message = new ChatMessage();
        message.setSenderId(messageDTO.getSenderId());
        message.setReceiverId(messageDTO.getReceiverId());
        message.setContent(messageDTO.getContent());
        message.setStatus(ChatMessage.MessageStatus.SENT);
        message.setTimestamp(LocalDateTime.now());

        return chatMessageRepository.save(message)
            .flatMap(savedMessage -> {
                logger.debug("Message saved to MongoDB: {}", savedMessage);
                
                // Check if receiver is online
                if (sessionManager.isUserOnline(messageDTO.getReceiverId())) {
                    // If online, send directly through WebSocket
                    WebSocketSession receiverSession = sessionManager.getSession(messageDTO.getReceiverId());
                    if (receiverSession != null && receiverSession.isOpen()) {
                        try {
                            String messageJson = objectMapper.writeValueAsString(convertToDTO(savedMessage));
                            return receiverSession.send(Mono.just(receiverSession.textMessage(messageJson)))
                                .doOnSuccess(v -> {
                                    logger.debug("Message sent directly via WebSocket to: {}", messageDTO.getReceiverId());
                                    // Mark message as delivered
                                    savedMessage.setStatus(ChatMessage.MessageStatus.DELIVERED);
                                    savedMessage.setDeliveredAt(LocalDateTime.now());
                                    chatMessageRepository.save(savedMessage).subscribe();
                                });
                        } catch (Exception e) {
                            logger.error("Error sending direct WebSocket message: {}", e.getMessage());
                            return Mono.empty();
                        }
                    }
                } else {
                    // If offline, store in Redis for later delivery
                    String offlineKey = OFFLINE_MESSAGES_KEY + messageDTO.getReceiverId();
                    try {
                        String messageJson = objectMapper.writeValueAsString(convertToDTO(savedMessage));
                        return redisTemplate.opsForList().rightPush(offlineKey, messageJson)
                            .doOnSuccess(v -> logger.debug("Message stored in Redis for offline user: {}", messageDTO.getReceiverId()));
                    } catch (Exception e) {
                        logger.error("Error storing offline message: {}", e.getMessage());
                        return Mono.empty();
                    }
                }
                return Mono.empty();
            })
            .then();
    }

    @Override
    public Mono<ChatMessage> getChatHistory(String senderId, String receiverId) {
        logger.debug("Getting chat history between {} and {}", senderId, receiverId);
        return chatMessageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderId(
            senderId, receiverId, senderId, receiverId
        )
        .doOnNext(message -> logger.debug("Found message: {}", message))
        .sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
        .next();
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

    @Override
    public Mono<Void> deliverOfflineMessages(String userId) {
        logger.debug("Delivering offline messages for user: {}", userId);
        String offlineKey = OFFLINE_MESSAGES_KEY + userId;
        
        return redisTemplate.opsForList().size(offlineKey)
            .flatMap(size -> {
                if (size > 0) {
                    logger.debug("Found {} offline messages for user: {}", size, userId);
                    return redisTemplate.opsForList().leftPop(offlineKey)
                        .flatMap(messageJson -> {
                            try {
                                ChatMessageDTO messageDTO = objectMapper.readValue(messageJson, ChatMessageDTO.class);
                                WebSocketSession session = sessionManager.getSession(userId);
                                
                                if (session != null && session.isOpen()) {
                                    return session.send(Mono.just(session.textMessage(messageJson)))
                                        .doOnSuccess(v -> {
                                            logger.debug("Offline message delivered to: {}", userId);
                                            // Mark message as delivered in MongoDB
                                            chatMessageRepository.findById(messageDTO.getId())
                                                .flatMap(message -> {
                                                    message.setStatus(ChatMessage.MessageStatus.DELIVERED);
                                                    message.setDeliveredAt(LocalDateTime.now());
                                                    return chatMessageRepository.save(message);
                                                })
                                                .subscribe();
                                        });
                                }
                                return Mono.empty();
                            } catch (Exception e) {
                                logger.error("Error processing offline message: {}", e.getMessage());
                                return Mono.empty();
                            }
                        })
                        .repeat(size - 1)
                        .then();
                }
                return Mono.empty();
            });
    }

    private ChatMessageDTO convertToDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        dto.setStatus(message.getStatus());
        return dto;
    }
} 