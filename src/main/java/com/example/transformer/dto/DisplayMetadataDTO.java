package com.example.transformer.dto;

import com.example.transformer.model.DisplayMetadata;
import java.time.LocalDateTime;
import java.util.Map;

public record DisplayMetadataDTO(
    Long id,
    Map<String, String> boxColors,
    LocalDateTime timestamp
) {
    public static DisplayMetadataDTO fromEntity(DisplayMetadata entity) {
        return new DisplayMetadataDTO(
            entity.getId(),
            entity.getBoxColors(),
            entity.getTimestamp()
        );
    }
}
