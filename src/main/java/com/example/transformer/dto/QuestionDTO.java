package com.example.transformer.dto;

import jakarta.validation.constraints.NotBlank;

public record QuestionDTO(
    @NotBlank String text
) {}
