package com.hellochat.backend.config;

import com.hellochat.backend.common.TokenProvider;
import com.hellochat.backend.websocket.ChatWebSocketHandler;
import com.hellochat.backend.websocket.TokenHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final TokenProvider tokenProvider;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler, TokenProvider tokenProvider) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
            .addInterceptors(new TokenHandshakeInterceptor(tokenProvider))
            .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}
