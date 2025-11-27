package com.example.transformer.controller;

import com.example.transformer.dto.CreateInspectionDTO;
import com.example.transformer.dto.InspectionDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.*;
import com.example.transformer.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/transformers")
public class InspectionController {

    private final TransformerRepository transformers;
    private final InspectionRepository inspectionRepository;
    private final TransformerImageRepository images;
    private final FaultRegionRepository faultRegionRepository;
    private final DisplayMetadataRepository displayMetadataRepository;
    private final OriginalAnomalyResultRepository originalAnomalyResultRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public InspectionController(TransformerRepository transformers,
                                InspectionRepository inspectionRepository,
                                TransformerImageRepository images,
                                FaultRegionRepository faultRegionRepository,
                                DisplayMetadataRepository displayMetadataRepository,
                                OriginalAnomalyResultRepository originalAnomalyResultRepository,
                                MaintenanceRecordRepository maintenanceRecordRepository) {
        this.transformers = transformers;
        this.inspectionRepository = inspectionRepository;
        this.images = images;
        this.faultRegionRepository = faultRegionRepository;
        this.displayMetadataRepository = displayMetadataRepository;
        this.originalAnomalyResultRepository = originalAnomalyResultRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    @GetMapping("/{id}/inspections")
    public List<InspectionDTO> listInspections(@PathVariable Long id) {
        if (!transformers.existsById(id))
            throw new NotFoundException("Transformer " + id + " not found");
        return inspectionRepository.findByTransformerIdOrderByCreatedAtDesc(id)
                .stream().map(InspectionDTO::fromEntity).toList();
    }

    @PostMapping("/{id}/inspections")
    public ResponseEntity<InspectionDTO> addInspection(@PathVariable Long id,
                                                       @RequestBody @Valid CreateInspectionDTO body) {
        Transformer t = transformers.findById(id)
                .orElseThrow(() -> new NotFoundException("Transformer " + id + " not found"));

        Inspection ins = Inspection.builder()
                .transformer(t)
                .title(body.title())
                .inspector(body.inspector())
                .notes(body.notes())
                .status(body.status() == null ? InspectionStatus.OPEN : body.status())
                .createdAt(LocalDateTime.now())
                .build();

        inspectionRepository.save(ins);
        return ResponseEntity.status(HttpStatus.CREATED).body(InspectionDTO.fromEntity(ins));
    }

    @DeleteMapping("/{transformerId}/inspections/{inspectionId}")
    public ResponseEntity<Void> deleteInspection(
            @PathVariable Long transformerId,
            @PathVariable Long inspectionId) {

        // 1. Check transformer exists
        Transformer t = transformers.findById(transformerId)
                .orElseThrow(() -> new NotFoundException("Transformer " + transformerId + " not found"));

        // 2. Load inspection
        Inspection ins = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new NotFoundException("Inspection " + inspectionId + " not found"));

        // 3. Ensure it belongs to this transformer
        if (ins.getTransformer() == null ||
                !Objects.equals(ins.getTransformer().getId(), t.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inspection does not belong to this transformer"
            );
        }

        // Clean up related data:
        maintenanceRecordRepository.deleteByInspectionId(inspectionId);

        List<TransformerImage> imgs = images.findByInspectionId(inspectionId);
        for (TransformerImage img : imgs) {

            originalAnomalyResultRepository.deleteByImageId(img.getId());
            faultRegionRepository.deleteByImageId(img.getId());
            displayMetadataRepository.deleteByImageId(img.getId());

            images.delete(img);
        }

        inspectionRepository.delete(ins);

        return ResponseEntity.noContent().build();
    }
}
