package com.shortnx.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Basic fixed-window rate limiter, in-memory.
 * Good enough for a single-instance resume project. For a real multi-server
 * deployment you'd back this with Redis/Supabase instead of a HashMap,
 * since in-memory state doesn't share across app instances — worth
 * mentioning in an interview as a known limitation.
 */
public final class RateLimiter {

    private static final int MAX_REQUESTS_PER_WINDOW = 20;
    private static final long WINDOW_MILLIS = 60_000; // 1 minute

    private static final ConcurrentHashMap<String, Window> BUCKETS = new ConcurrentHashMap<>();

    private RateLimiter() {
    }

    public static boolean allow(String key) {
        long now = System.currentTimeMillis();
        Window window = BUCKETS.compute(key, (k, existing) -> {
            if (existing == null || now - existing.start > WINDOW_MILLIS) {
                return new Window(now);
            }
            return existing;
        });
        return window.count.incrementAndGet() <= MAX_REQUESTS_PER_WINDOW;
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long start) {
            this.start = start;
        }
    }
}
