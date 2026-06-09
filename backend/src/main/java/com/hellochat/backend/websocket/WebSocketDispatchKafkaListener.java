package com.hellochat.backend.websocket;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WebSocketDispatchKafkaListener {

    private final WebSocketDispatchDispatcher dispatcher;

    public WebSocketDispatchKafkaListener(WebSocketDispatchDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaListener(
        topics = "${hello-chat.concurrency.websocket-dispatch-topic}",
        groupId = "${hello-chat.concurrency.node-id}-websocket-dispatch"
    )
    public void consume(WebSocketDispatchEvent event) {
        dispatcher.dispatch(event);
    }
}
