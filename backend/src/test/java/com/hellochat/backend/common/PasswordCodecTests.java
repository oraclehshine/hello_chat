package com.hellochat.backend.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordCodecTests {

    private final PasswordCodec passwordCodec = new PasswordCodec();

    @Test
    void encodedPasswordMatchesOriginalPassword() {
        String encoded = passwordCodec.encode("Password123");

        assertTrue(passwordCodec.matches("Password123", encoded));
        assertFalse(passwordCodec.matches("WrongPassword123", encoded));
    }

    @Test
    void encodeUsesDifferentSaltEachTime() {
        String first = passwordCodec.encode("Password123");
        String second = passwordCodec.encode("Password123");

        assertNotEquals(first, second);
    }
}
