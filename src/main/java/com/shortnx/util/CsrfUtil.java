package com.shortnx.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SESSION_KEY = "csrfToken";

    private CsrfUtil() {
    }

    /** Call this once per session (e.g. when rendering a form) to get a token to embed. */
    public static String getOrCreateToken(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_KEY);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_KEY, token);
        }
        return token;
    }

    /** Call this on every state-changing POST before doing anything else. */
    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        String sessionToken = (String) session.getAttribute(SESSION_KEY);
        String submitted = request.getParameter("csrfToken");
        return sessionToken != null && sessionToken.equals(submitted);
    }
}
