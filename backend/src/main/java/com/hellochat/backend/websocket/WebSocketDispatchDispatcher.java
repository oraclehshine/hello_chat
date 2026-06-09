package com.hellochat.backend.websocket;

import com.hellochat.backend.config.HighConcurrencyProperties;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WebSocketDispatchDispatcher {

    private final UserSessionRegistry userSessionRegistry;
    private final HighConcurrencyProperties properties;

    public WebSocketDispatchDispatcher(
        UserSessionRegistry userSessionRegistry,
        HighConcurrencyProperties properties
    ) {
        this.userSessionRegistry = userSessionRegistry;
        this.properties = properties;
    }

    public void dispatch(WebSocketDispatchEvent event) {
        if (event == null || event.getUserId() == null || event.getPayload() == null) {
            return;
        }
        Set<String> routeNodes = userSessionRegistry.getRouteNodes(event.getUserId());
        if (!routeNodes.isEmpty() && !routeNodes.contains(properties.getNodeId())) {
            return;
        }
        userSessionRegistry.pushToUser(event.getUserId(), event.getPayload());
    }
}
