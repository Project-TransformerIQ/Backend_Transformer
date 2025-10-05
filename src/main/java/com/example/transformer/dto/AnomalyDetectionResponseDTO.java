package com.example.transformer.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AnomalyDetectionResponseDTO(
    List<FaultRegionDTO> faultRegions,
    DisplayMetadataDTO displayMetadata,
    LocalDateTime timestamp
) {
    public record FaultRegionDTO(
        Integer id,
        String type,
        String dominantColor,
        List<Integer> colorRgb,
        BoundingBoxDTO boundingBox,
        CentroidDTO centroid,
        Double aspectRatio,
        Boolean elongated,
        Boolean connectedToWire,
        String tag,
        Double confidence
    ) {}

    public record BoundingBoxDTO(
        Integer x,
        Integer y,
        Integer width,
        Integer height,
        Integer areaPx
    ) {}

    public record CentroidDTO(
        Integer x,
        Integer y
    ) {}

    public record DisplayMetadataDTO(
        Map<String, List<Integer>> boxColors
    ) {}
}
