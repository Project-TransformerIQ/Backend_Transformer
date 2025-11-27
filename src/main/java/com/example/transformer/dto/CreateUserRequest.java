// src/main/java/com/example/transformer/dto/CreateUserRequest.java
package com.example.transformer.dto;

import com.example.transformer.model.UserOccupation;

public record CreateUserRequest(
        String name,
        String password,
        UserOccupation occupation
) {}
