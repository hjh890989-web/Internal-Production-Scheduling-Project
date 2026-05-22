package com.scheduling.notify;

import com.scheduling.ex.events.ExReplanCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * ExReplanPushListener — TK-EX14-1-2 (BR-EX14).
 */
class ExReplanPushListenerTest {

    @Test
    @DisplayName("ExReplanCompletedEvent → SimpMessagingTemplate /topic/extrusion-updates send")
    void publishes_to_extrusion_updates_topic() {
        SimpMessagingTemplate stomp = mock(SimpMessagingTemplate.class);
        ExReplanPushListener listener = new ExReplanPushListener(stomp);

        ExReplanCompletedEvent event = new ExReplanCompletedEvent(
            UUID.randomUUID(), Instant.now(), 3,
            List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        listener.onReplanCompleted(event);

        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(stomp, times(1)).convertAndSend(destCaptor.capture(), payloadCaptor.capture());
        assertThat(destCaptor.getValue()).isEqualTo("/topic/extrusion-updates");
        assertThat(payloadCaptor.getValue()).isSameAs(event);
    }

    @Test
    @DisplayName("EXTRUSION_UPDATES_TOPIC 상수 — /topic/extrusion-updates (BR-EX14)")
    void topic_constant_unchanged() {
        assertThat(ExReplanPushListener.EXTRUSION_UPDATES_TOPIC)
            .isEqualTo("/topic/extrusion-updates");
    }
}
