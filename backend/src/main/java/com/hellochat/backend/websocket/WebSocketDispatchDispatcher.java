package com.hellochat.backend.websocket;

import com.hellochat.backend.cache.KafkaEventDedupService;
import com.hellochat.backend.config.HighConcurrencyProperties;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WebSocketDispatchDispatcher {

    private final UserSessionRegistry userSessionRegistry;
    private final HighConcurrencyProperties properties;
    private final KafkaEventDedupService kafkaEventDedupService;

    public WebSocketDispatchDispatcher(
        UserSessionRegistry userSessionRegistry,
        HighConcurrencyProperties properties,
        KafkaEventDedupService kafkaEventDedupService
    ) {
        this.userSessionRegistry = userSessionRegistry;
        this.properties = properties;
        this.kafkaEventDedupService = kafkaEventDedupService;
    }

    public void dispatch(WebSocketDispatchEvent event) {
        if (event == null || event.getUserId() == null || event.getPayload() == null) {
            return;
        }
        if (!kafkaEventDedupService.shouldProcess("websocket-dispatch", event.getEventId())) {
            return;
        }
        Set<String> routeNodes = userSessionRegistry.getRouteNodes(event.getUserId());
        if (!routeNodes.isEmpty() && !routeNodes.contains(properties.getNodeId())) {
            return;
        }
        userSessionRegistry.pushToUser(event.getUserId(), event.getPayload());
    }
}
