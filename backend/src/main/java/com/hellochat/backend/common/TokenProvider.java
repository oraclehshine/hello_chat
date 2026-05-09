package com.hellochat.backend.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;
    private final long accessTokenSeconds;

    public TokenProvider(
        @Value("${hello-chat.auth.secret:hello-chat-local-secret}") String secret,
        @Value("${hello-chat.auth.access-token-seconds:7200}") long accessTokenSeconds
    ) {
        this.secret = secret;
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public String createAccessToken(Long userId) {
        long expiresAt = Instant.now().plusSeconds(accessTokenSeconds).getEpochSecond();
        String payload = userId + "." + expiresAt;
        return payload + "." + sign(payload);
    }

    public Long parseUserId(String token) {
        String[] parts = token.split("\\.", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("token invalid");
        }
        String payload = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(payload), parts[2])) {
            throw new IllegalArgumentException("token invalid");
        }
        long expiresAt = Long.parseLong(parts[1]);
        if (expiresAt < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("token expired");
        }
        return Long.valueOf(parts[0]);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token", ex);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
