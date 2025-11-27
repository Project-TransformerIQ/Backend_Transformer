// src/main/java/com/example/transformer/dto/LoginResponse.java
package com.example.transformer.dto;

import com.example.transformer.model.UserOccupation;

public record LoginResponse(
        String token,
        Long id,
        String name,
        UserOccupation occupation,
        boolean admin
) {}
