// src/main/java/com/example/transformer/dto/LoginRequest.java
package com.example.transformer.dto;

public record LoginRequest(
        String name,
        String password
) {}
