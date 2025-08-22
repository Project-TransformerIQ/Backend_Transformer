package com.example.transformer.dto;

import com.example.transformer.model.TransformerType;
import jakarta.validation.constraints.*;

public record TransformerDTO(
    Long id,
    @NotBlank String transformerNo,
    @NotBlank String poleNo,
    @NotBlank String region,
    @NotNull TransformerType transformerType
) {}
