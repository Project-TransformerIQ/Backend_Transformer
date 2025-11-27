package com.example.transformer.dto;

import com.example.transformer.model.FaultRegion;
import java.util.List;

public record FaultRegionDTO(
    Long dbId,
    Integer regionId,
    String type,
    String dominantColor,
    List<Integer> colorRgb,
    BoundingBoxDTO boundingBox,
    CentroidDTO centroid,
    Double aspectRatio,
    Boolean elongated,
    Boolean connectedToWire,
    String tag,
    Double confidence,
    String created_by
) {
    public static FaultRegionDTO fromEntity(FaultRegion entity) {
        BoundingBoxDTO boundingBoxDTO = null;
        if (entity.getBoundingBox() != null) {
            FaultRegion.BoundingBox bb = entity.getBoundingBox();
            boundingBoxDTO = new BoundingBoxDTO(bb.getX(), bb.getY(), bb.getWidth(), bb.getHeight(), bb.getAreaPx());
        }

        CentroidDTO centroidDTO = null;
        if (entity.getCentroid() != null) {
            FaultRegion.Centroid c = entity.getCentroid();
            centroidDTO = new CentroidDTO(c.getX(), c.getY());
        }

        return new FaultRegionDTO(
            entity.getDbId(),
            entity.getRegionId(),
            entity.getType(),
            entity.getDominantColor(),
            entity.getColorRgb(),
            boundingBoxDTO,
            centroidDTO,
            entity.getAspectRatio(),
            entity.getElongated(),
            entity.getConnectedToWire(),
            entity.getTag(),
            entity.getConfidence(),
            entity.getCreatedBy()
        );
    }

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
}
