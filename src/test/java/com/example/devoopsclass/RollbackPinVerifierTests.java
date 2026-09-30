package com.example.devoopsclass;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RollbackPinVerifierTests {
    @Test
    void verifiesOnlyTheConfiguredEightDigitPin() throws Exception {
        String pin = "04268193";
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        int iterations = 310_000;
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, iterations, 256);
        byte[] hash;
        try {
            hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
        String encoded = "pbkdf2_sha256$" + iterations + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
        RollbackPinVerifier verifier = new RollbackPinVerifier(encoded);

        assertTrue(verifier.verify(pin));
        assertFalse(verifier.verify("00000000"));
        assertFalse(verifier.verify("1234"));
        assertFalse(new RollbackPinVerifier("invalid").verify(pin));
    }
}
