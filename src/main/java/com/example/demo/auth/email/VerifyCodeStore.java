package com.example.demo.auth.email;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerifyCodeStore {

    private static final long EXPIRY_SECONDS = 300; // 5분
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_SECONDS = 300; // 5분 잠금

    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LockEntry> locks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> failCounts = new ConcurrentHashMap<>();

    public void save(String email, String code) {
        store.put(email, new CodeEntry(code, Instant.now().plusSeconds(EXPIRY_SECONDS)));
        failCounts.remove(email);
    }

    public Optional<String> find(String email) {
        CodeEntry entry = store.get(email);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(email);
            return Optional.empty();
        }
        return Optional.of(entry.code());
    }

    public void remove(String email) {
        store.remove(email);
        failCounts.remove(email);
    }

    public boolean isLocked(String email) {
        LockEntry lock = locks.get(email);
        if (lock == null) {
            return false;
        }
        if (Instant.now().isAfter(lock.expiresAt())) {
            locks.remove(email);
            return false;
        }
        return true;
    }

    public void recordFailure(String email) {
        int count = failCounts.merge(email, 1, Integer::sum);
        if (count >= MAX_ATTEMPTS) {
            locks.put(email, new LockEntry(Instant.now().plusSeconds(LOCK_SECONDS)));
            failCounts.remove(email);
            store.remove(email);
        }
    }

    public int getFailCount(String email) {
        return failCounts.getOrDefault(email, 0);
    }

    record CodeEntry(String code, Instant expiresAt) {}

    record LockEntry(Instant expiresAt) {}
}
