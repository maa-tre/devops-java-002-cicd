package com.example.devoopsclass;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@Component
public class RollbackPinVerifier {
    private final String pinHash;

    public RollbackPinVerifier(@Value("${ROLLBACK_PIN_HASH:}") String pinHash) {
        this.pinHash = pinHash;
    }

    public boolean verify(String pin) {
        if (pin == null || pin.length() < 8 || !pin.matches("[0-9]+") || pinHash.isBlank()) {
            return false;
        }
        String[] parts = pinHash.split("\\$", -1);
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 2_000_000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, iterations, expected.length * 8);
            byte[] actual;
            try {
                actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                        .generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException | InvalidKeySpecException | java.security.NoSuchAlgorithmException ex) {
            return false;
        }
    }
}
