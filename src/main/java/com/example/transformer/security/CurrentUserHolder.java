// src/main/java/com/example/transformer/security/CurrentUserHolder.java
package com.example.transformer.security;

public class CurrentUserHolder {

    private static final ThreadLocal<SessionUser> CURRENT = new ThreadLocal<>();

    public static void set(SessionUser user) {
        CURRENT.set(user);
    }

    public static SessionUser get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
