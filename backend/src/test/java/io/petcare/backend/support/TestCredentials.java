package io.petcare.backend.support;

import java.security.SecureRandom;
import java.util.Base64;

public final class TestCredentials {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TestCredentials() {
    }

    public static String newPassword() {
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret) + "Aa1!";
    }
}
