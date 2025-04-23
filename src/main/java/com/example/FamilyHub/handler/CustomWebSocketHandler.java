package com.example.FamilyHub.handler;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public class CustomWebSocketHandler implements WebSocketHandler {

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        System.out.println("New WebSocket connection established");
        
        return session.receive()
            .doOnNext(message -> {
                String payload = message.getPayloadAsText();
                System.out.println("Received message: " + payload);
            })
            .map(WebSocketMessage::getPayloadAsText)
            .flatMap(message -> session.send(
                Mono.just(session.textMessage("Echo: " + message))
            ))
            .doOnError(error -> {
                System.err.println("WebSocket error: " + error.getMessage());
            })
            .doFinally(signalType -> {
                System.out.println("WebSocket connection closed: " + signalType);
            })
            .then();
    }
} 