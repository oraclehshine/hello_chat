package com.hellochat.backend.websocket;

import com.hellochat.backend.common.TokenProvider;
import java.net.URI;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class TokenHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";

    private final TokenProvider tokenProvider;

    public TokenHandshakeInterceptor(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        String token = extractToken(request.getURI());
        Long userId = tokenProvider.parseUserId(token);
        attributes.put(USER_ID_ATTRIBUTE, userId);
        return true;
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception
    ) {
    }

    private String extractToken(URI uri) {
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && "token".equals(pair[0]) && !pair[1].isBlank()) {
                return pair[1];
            }
        }
        throw new IllegalArgumentException("token is required");
    }
}
