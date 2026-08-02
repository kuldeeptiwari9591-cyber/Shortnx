package com.shortnx.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Centralised input validation. Every servlet must run untrusted input
 * through these before it touches the database or gets reflected into HTML.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // letters, numbers, dash, underscore only — blocks path traversal /
    // header injection via a crafted alias
    private static final Pattern ALIAS_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{3,20}$");

    private ValidationUtil() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= 255 && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        // Minimum bar for a resume project: 8+ chars. Tighten with
        // upper/lower/digit checks if you want to show more rigor.
        return password != null && password.length() >= 8 && password.length() <= 72;
        // 72 is bcrypt's own input limit
    }

    public static boolean isValidAlias(String alias) {
        return alias == null || ALIAS_PATTERN.matcher(alias).matches();
    }

    /**
     * Validates that the submitted long URL is well-formed AND uses
     * http/https. Blocking other schemes stops things like
     * javascript: or file: being stored and later "opened" by a client
     * that trusts the redirect blindly, and stops the service being used
     * as an open redirector for arbitrary protocols.
     */
    public static boolean isValidLongUrl(String url) {
        if (url == null || url.isBlank() || url.length() > 2048) {
            return false;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            scheme = scheme.toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) return false;
            if (uri.getHost() == null || uri.getHost().isBlank()) return false;
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Escapes text for safe insertion into HTML. Used anywhere a
     * user-supplied value (long URL, alias, email) is echoed back into a
     * page, to prevent stored/reflected XSS.
     */
    public static String escapeHtml(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
