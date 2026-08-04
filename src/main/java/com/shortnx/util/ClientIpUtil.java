package com.shortnx.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Render (like most PaaS platforms) terminates the connection at a
 * reverse proxy, so HttpServletRequest.getRemoteAddr() returns the
 * proxy's internal IP for every single visitor — not the real client.
 * Left unfixed, every user shares one rate-limit bucket and one
 * "IP hash" in click logs, which is both wrong for analytics and
 * makes the rate limiter trip on legitimate traffic almost immediately.
 */
public final class ClientIpUtil {

    private ClientIpUtil() {
    }

    public static String getClientIp(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // X-Forwarded-For can be a comma-separated chain of proxies;
            // the first entry is the original client.
            return forwardedFor.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
