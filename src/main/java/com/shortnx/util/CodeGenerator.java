package com.shortnx.util;

import java.security.SecureRandom;

/**
 * Generates short codes for links.
 * Uses SecureRandom (not Math.random) because codes are effectively
 * access tokens for un-listed URLs — predictable codes let an attacker
 * enumerate other users' links.
 */
public final class CodeGenerator {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_LENGTH = 7;

    private CodeGenerator() {
    }

    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public static String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
