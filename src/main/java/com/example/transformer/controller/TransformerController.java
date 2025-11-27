package com.example.transformer.controller;

import com.example.transformer.dto.TransformerDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.Inspection;
import com.example.transformer.model.Transformer;
import com.example.transformer.model.TransformerImage;
import com.example.transformer.repository.DisplayMetadataRepository;
import com.example.transformer.repository.FaultRegionRepository;
import com.example.transformer.repository.InspectionRepository;
import com.example.transformer.repository.MaintenanceRecordRepository;
import com.example.transformer.repository.OriginalAnomalyResultRepository;
import com.example.transformer.repository.TransformerImageRepository;
import com.example.transformer.repository.TransformerRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transformers")
public class TransformerController {

    private final TransformerRepository transformers;
    private final TransformerImageRepository images;
    private final InspectionRepository inspectionRepository;
    private final FaultRegionRepository faultRegionRepository;
    private final DisplayMetadataRepository displayMetadataRepository;
    private final OriginalAnomalyResultRepository originalAnomalyResultRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public TransformerController(TransformerRepository transformers,
                                 TransformerImageRepository images,
                                 InspectionRepository inspectionRepository,
                                 FaultRegionRepository faultRegionRepository,
                                 DisplayMetadataRepository displayMetadataRepository,
                                 OriginalAnomalyResultRepository originalAnomalyResultRepository,
                                 MaintenanceRecordRepository maintenanceRecordRepository) {
        this.transformers = transformers;
        this.images = images;
        this.inspectionRepository = inspectionRepository;
        this.faultRegionRepository = faultRegionRepository;
        this.displayMetadataRepository = displayMetadataRepository;
        this.originalAnomalyResultRepository = originalAnomalyResultRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    // ---- CRUD: transformers ----
    @PostMapping
    public Transformer create(@RequestBody @Valid TransformerDTO dto) {
        Transformer t = Transformer.builder()
                .transformerNo(dto.transformerNo())
                .poleNo(dto.poleNo())
                .region(dto.region())
                .transformerType(dto.transformerType())
                .build();
        return transformers.save(t);
    }

    @GetMapping
    public List<Transformer> list() {
        return transformers.findAll();
    }

    @GetMapping("/{id}")
    public Transformer get(@PathVariable Long id) {
        return transformers.findById(id)
                .orElseThrow(() -> new NotFoundException("Transformer " + id + " not found"));
    }

    @PutMapping("/{id}")
    public Transformer update(@PathVariable Long id, @RequestBody @Valid TransformerDTO dto) {
        Transformer t = get(id);
        t.setTransformerNo(dto.transformerNo());
        t.setPoleNo(dto.poleNo());
        t.setRegion(dto.region());
        t.setTransformerType(dto.transformerType());
        return transformers.save(t);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        // 1. Load transformer or 404
        Transformer t = transformers.findById(id)
                .orElseThrow(() -> new NotFoundException("Transformer " + id + " not found"));

        // 2. Clean up all inspections belonging to this transformer
        List<Inspection> inspections = inspectionRepository
                .findByTransformerIdOrderByCreatedAtDesc(id);

        for (Inspection ins : inspections) {
            Long inspectionId = ins.getId();

            // 2a. Delete maintenance records linked to this inspection
            maintenanceRecordRepository.deleteByInspectionId(inspectionId);

            // 2b. Delete images linked to this inspection
            List<TransformerImage> imgs = images.findByInspectionId(inspectionId);
            for (TransformerImage img : imgs) {
                Long imgId = img.getId();

                // Delete anomaly-related data for this image
                originalAnomalyResultRepository.deleteByImageId(imgId);
                faultRegionRepository.deleteByImageId(imgId);
                displayMetadataRepository.deleteByImageId(imgId);

                // Delete any maintenance records tied specifically to this image
                maintenanceRecordRepository.deleteByMaintenanceImageId(imgId);

                // Finally delete the image itself
                images.delete(img);
            }

            // 2c. Delete the inspection row itself
            inspectionRepository.delete(ins);
        }

        // 3. Clean up any remaining images for this transformer (e.g. baseline images not tied to inspections)
        List<TransformerImage> remainingImages = images.findByTransformerIdOrderByCreatedAtDesc(id);
        for (TransformerImage img : remainingImages) {
            Long imgId = img.getId();

            originalAnomalyResultRepository.deleteByImageId(imgId);
            faultRegionRepository.deleteByImageId(imgId);
            displayMetadataRepository.deleteByImageId(imgId);
            maintenanceRecordRepository.deleteByMaintenanceImageId(imgId);

            images.delete(img);
        }

        // 4. As extra safety, delete any remaining maintenance records directly linked to this transformer
        maintenanceRecordRepository.deleteByTransformerId(id);

        // 5. Finally delete the transformer itself
        transformers.delete(t);

        return ResponseEntity.noContent().build();
    }
}
