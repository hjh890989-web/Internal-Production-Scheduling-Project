package com.scheduling.notify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub STOMP fan-out — TK-41-4-1 (Sprint 6 인프라, 다중 인스턴스 scale-out).
 *
 * <p>Spring Boot {@code @EnableWebSocketMessageBroker} simple in-memory broker 는
 * 단일 인스턴스 한정. 다중 인스턴스 시 한 인스턴스의 publish 가 다른 인스턴스의 subscriber 에
 * 전달되지 않음. 본 config 는 Redis Pub/Sub 으로 inter-instance fan-out.
 *
 * <p>Sprint 6 baseline — config + RedisMessageListenerContainer 활성. 본격 fan-out 은
 * STOMP relay (RabbitMQ/ActiveMQ) 도입 시 Sprint 7+. 현재는 toggle 가능 config 만 제공.
 *
 * <p>{@code scheduling.notify.redis-fanout.enabled=false} (기본) — 단일 인스턴스 운영.
 * {@code =true} + Redis 활성 시 inter-instance STOMP fan-out 활성 (Phase 2+).
 */
@Configuration
@Profile("with-infra")
public class RedisStompFanoutConfig {

    @Value("${scheduling.notify.redis-fanout.enabled:false}")
    private boolean enabled;

    /**
     * Redis Pub/Sub listener container — multi-instance STOMP fan-out 진입점.
     *
     * <p>현재 stub — Sprint 7+ 다중 인스턴스 운영 시 enabled=true + 본격 listener 등록.
     */
    @Bean
    public RedisMessageListenerContainer redisStompFanoutListener(RedisConnectionFactory cf) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(cf);
        // Sprint 7+ — container.addMessageListener(stompFanoutListener, new ChannelTopic("stomp.fanout"))
        if (!enabled) {
            container.afterPropertiesSet();
            // 활성 안 함 — Spring lifecycle 만 등록 (다중 instance 운영 시 enabled=true)
        }
        return container;
    }

    /**
     * 다중 instance 간 publish — 다른 instance 의 simple broker 에 전파.
     */
    @Bean
    public StringRedisTemplate stompFanoutTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    public boolean isEnabled() { return enabled; }
}
