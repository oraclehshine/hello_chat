package com.hellochat.backend.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TokenProviderTests {

    @Test
    void tokenRoundTripsUserId() {
        TokenProvider tokenProvider = new TokenProvider("test-secret", 60);

        String token = tokenProvider.createAccessToken(42L);

        assertEquals(42L, tokenProvider.parseUserId(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        TokenProvider tokenProvider = new TokenProvider("test-secret", 60);
        String token = tokenProvider.createAccessToken(42L);
        String tampered = token.replaceFirst("42", "43");

        assertThrows(IllegalArgumentException.class, () -> tokenProvider.parseUserId(tampered));
    }
}
