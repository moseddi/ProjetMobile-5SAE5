package com.hangout.ai;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleRateLimiter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public void check(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > WINDOW_MS) {
                queue.pollFirst();
            }
            if (queue.size() >= MAX_REQUESTS) {
                throw new AiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Trop de demandes, réessayez dans une minute");
            }
            queue.addLast(now);
        }
    }
}