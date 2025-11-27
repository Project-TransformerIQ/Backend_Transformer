// src/main/java/com/example/transformer/security/SessionService.java
package com.example.transformer.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final Map<String, SessionUser> sessions = new ConcurrentHashMap<>();

    public String createSession(SessionUser user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    public Optional<SessionUser> getUser(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return Optional.ofNullable(sessions.get(token));
    }

    public void invalidate(String token) {
        if (token != null) sessions.remove(token);
    }
}
