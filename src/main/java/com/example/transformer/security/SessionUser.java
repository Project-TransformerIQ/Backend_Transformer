// src/main/java/com/example/transformer/security/SessionUser.java
package com.example.transformer.security;

import com.example.transformer.model.UserOccupation;

public record SessionUser(
        Long id,
        String name,
        UserOccupation occupation,
        boolean admin
) {}
