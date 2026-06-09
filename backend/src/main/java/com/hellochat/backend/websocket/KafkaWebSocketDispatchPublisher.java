package com.hellochat.backend.websocket;

import com.hellochat.backend.config.HighConcurrencyProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaWebSocketDispatchPublisher implements WebSocketDispatchPublisher {

    private final KafkaTemplate<String, WebSocketDispatchEvent> kafkaTemplate;
    private final WebSocketDispatchDispatcher dispatcher;
    private final HighConcurrencyProperties properties;

    public KafkaWebSocketDispatchPublisher(
        KafkaTemplate<String, WebSocketDispatchEvent> kafkaTemplate,
        WebSocketDispatchDispatcher dispatcher,
        HighConcurrencyProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.dispatcher = dispatcher;
        this.properties = properties;
    }

    @Override
    public void publish(Long userId, String eventType, String payload) {
        WebSocketDispatchEvent event = new WebSocketDispatchEvent(
            userId,
            eventType,
            payload,
            properties.getNodeId()
        );
        if (!properties.isWebsocketKafkaEnabled()) {
            dispatcher.dispatch(event);
            return;
        }
        kafkaTemplate.send(properties.getWebsocketDispatchTopic(), String.valueOf(userId), event)
            .whenComplete((ignored, ex) -> {
                if (ex != null) {
                    dispatcher.dispatch(event);
                }
            });
    }
}
