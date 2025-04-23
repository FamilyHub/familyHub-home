package com.example.FamilyHub.handler;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public class ReactiveWebSocketHandler implements WebSocketHandler {

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.receive()
            .map(webSocketMessage -> webSocketMessage.getPayloadAsText())
            .flatMap(message -> session.send(
                Mono.just(session.textMessage("Echo: " + message))
            ))
            .then();
    }
} 