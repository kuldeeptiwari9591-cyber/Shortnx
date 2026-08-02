package com.shortnx.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Never store or compare plaintext passwords. BCrypt is salted and slow
 * by design, which is what you want for password hashing (unlike SHA-256,
 * which is fast and therefore bad for this).
 */
public final class PasswordUtil {

    private static final int WORK_FACTOR = 12; // higher = slower = more brute-force resistant

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean verify(String plainPassword, String hash) {
        try {
            return BCrypt.checkpw(plainPassword, hash);
        } catch (IllegalArgumentException e) {
            // malformed hash in DB — fail closed
            return false;
        }
    }
}
