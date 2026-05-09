package com.hellochat.backend.common;

import jakarta.servlet.http.HttpServletRequest;

public final class CurrentUser {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private CurrentUser() {
    }

    public static Long requireUserId(HttpServletRequest request, TokenProvider tokenProvider) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return tokenProvider.parseUserId(authorization.substring(BEARER_PREFIX.length()));
        }
        return requireUserId(request);
    }

    public static Long requireUserId(HttpServletRequest request) {
        String value = request.getHeader(USER_ID_HEADER);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("X-User-Id header must be a number");
        }
    }
}
