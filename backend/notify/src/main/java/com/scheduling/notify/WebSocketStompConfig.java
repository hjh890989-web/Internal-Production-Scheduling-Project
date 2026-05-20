package com.scheduling.notify;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP WebSocket 브로커 — TK-03-3-1 (REQ-FUNC-EX-014).
 *
 * <p>SockJS endpoint {@code /ws/notifications} + simple in-memory broker.
 * 클라이언트는 {@code SUBSCRIBE /topic/notifications/{role}} 로 알림 수신.
 *
 * <p>{@code @Profile("with-infra")} — Sprint 1 baseline DEV 컨텍스트 (DB 미연동) 에서는
 * WebSocket 미활성화. ST-30-2 (Keycloak JWT) 와 함께 활성됨.
 */
@Configuration
@Profile("with-infra")
@EnableWebSocketMessageBroker
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}
