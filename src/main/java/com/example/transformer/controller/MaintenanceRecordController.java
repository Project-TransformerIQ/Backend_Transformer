package com.example.transformer.controller;

import com.example.transformer.dto.*;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.*;
import com.example.transformer.repository.*;
import com.example.transformer.security.CurrentUserHolder;
import com.example.transformer.security.SessionUser;
import com.example.transformer.model.UserOccupation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/transformers")
public class MaintenanceRecordController {

    private final TransformerRepository transformers;
    private final TransformerImageRepository images;
    private final InspectionRepository inspectionRepository;
    private final FaultRegionRepository faultRegionRepository;
    private final DisplayMetadataRepository displayMetadataRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public MaintenanceRecordController(TransformerRepository transformers,
                                       TransformerImageRepository images,
                                       InspectionRepository inspectionRepository,
                                       FaultRegionRepository faultRegionRepository,
                                       DisplayMetadataRepository displayMetadataRepository,
                                       MaintenanceRecordRepository maintenanceRecordRepository) {
        this.transformers = transformers;
        this.images = images;
        this.inspectionRepository = inspectionRepository;
        this.faultRegionRepository = faultRegionRepository;
        this.displayMetadataRepository = displayMetadataRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    // FR4.1: Maintenance Record Form
    @GetMapping("/{id}/maintenance-record-form")
    public ResponseEntity<MaintenanceRecordFormDTO> getMaintenanceRecordForm(
            @PathVariable Long id,
            @RequestParam(value = "inspectionId", required = false) Long inspectionId,
            @RequestParam(value = "imageId", required = false) Long imageId) {

        Transformer t = transformers.findById(id)
                .orElseThrow(() -> new NotFoundException("Transformer " + id + " not found"));

        Inspection inspection = null;
        if (inspectionId != null) {
            inspection = inspectionRepository.findById(inspectionId)
                    .orElseThrow(() -> new NotFoundException("Inspection " + inspectionId + " not found"));
            if (inspection.getTransformer() == null ||
                    !Objects.equals(inspection.getTransformer().getId(), id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Inspection does not belong to this transformer");
            }
        }

        TransformerImage maintenanceImage = null;
        if (imageId != null) {
            maintenanceImage = images.findById(imageId)
                    .orElseThrow(() -> new NotFoundException("Image " + imageId + " not found"));
            if (!Objects.equals(maintenanceImage.getTransformer().getId(), id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Image does not belong to this transformer");
            }
            if (maintenanceImage.getImageType() != ImageType.MAINTENANCE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Maintenance record form requires a MAINTENANCE image");
            }
            if (inspection == null && maintenanceImage.getInspection() != null) {
                inspection = maintenanceImage.getInspection();
            }
        } else {
            List<TransformerImage> allMaint;

            if (inspection != null) {
                allMaint = images.findByTransformerIdAndImageTypeAndInspection_IdOrderByCreatedAtDesc(
                        id,
                        ImageType.MAINTENANCE,
                        inspection.getId()
                );
            } else {
                allMaint = images.findByTransformerIdAndImageTypeOrderByCreatedAtDesc(
                        id,
                        ImageType.MAINTENANCE
                );
            }

            if (!allMaint.isEmpty()) {
                maintenanceImage = allMaint.get(0);

                if (inspection == null) {
                    inspection = maintenanceImage.getInspection();
                }
            }
        }

        if (maintenanceImage == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No maintenance image found or provided for this transformer");
        }

        List<FaultRegion> faultRegions = faultRegionRepository
                .findByImageIdOrderByRegionIdAsc(maintenanceImage.getId());
        List<FaultRegionDTO> anomalyDTOs = faultRegions.stream()
                .map(FaultRegionDTO::fromEntity)
                .toList();

        Map<String, String> boxColors = new HashMap<>();
        Optional<DisplayMetadata> dmOpt = displayMetadataRepository.findByImageId(maintenanceImage.getId());
        if (dmOpt.isPresent() && dmOpt.get().getBoxColors() != null) {
            boxColors.putAll(dmOpt.get().getBoxColors());
        }

        MaintenanceRecordDTO existingRecordDTO = maintenanceRecordRepository
                .findByMaintenanceImageId(maintenanceImage.getId())
                .map(MaintenanceRecordDTO::fromEntity)
                .orElse(null);

        TransformerDTO transformerDTO = new TransformerDTO(
                t.getId(),
                t.getTransformerNo(),
                t.getPoleNo(),
                t.getRegion(),
                t.getTransformerType()
        );

        InspectionDTO inspectionDTO = (inspection == null) ? null : InspectionDTO.fromEntity(inspection);

        TransformerImageDTO imageDTO = new TransformerImageDTO(
                maintenanceImage.getId(),
                maintenanceImage.getImageType(),
                maintenanceImage.getUploader(),
                maintenanceImage.getEnvCondition(),
                maintenanceImage.getFilename(),
                maintenanceImage.getCreatedAt(),
                maintenanceImage.getContentType(),
                maintenanceImage.getSizeBytes(),
                maintenanceImage.getInspection() == null ? null : maintenanceImage.getInspection().getId()
        );

        MaintenanceRecordFormDTO formDTO = new MaintenanceRecordFormDTO(
                transformerDTO,
                inspectionDTO,
                imageDTO,
                anomalyDTOs,
                boxColors,
                Arrays.asList(MaintenanceStatus.values()),
                existingRecordDTO
        );

        return ResponseEntity.ok(formDTO);
    }

    // FR4.2 / FR4.3: Create maintenance record
    @PostMapping("/{id}/maintenance-records")
    public ResponseEntity<MaintenanceRecordDTO> createMaintenanceRecord(
            @PathVariable Long id,
            @RequestBody @Valid CreateMaintenanceRecordDTO body) {

        SessionUser current = CurrentUserHolder.get();
        if (current == null || current.occupation() != UserOccupation.MAINTENANCE_ENGINEER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only maintenance engineers can modify maintenance records"
            );
        }

        if (!id.equals(body.transformerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transformer ID in path does not match transformerId in body");
        }

        Transformer transformer = transformers.findById(id)
                .orElseThrow(() -> new NotFoundException("Transformer " + id + " not found"));

        TransformerImage image = images.findById(body.maintenanceImageId())
                .orElseThrow(() -> new NotFoundException("Image " + body.maintenanceImageId() + " not found"));

        if (!Objects.equals(image.getTransformer().getId(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maintenance image does not belong to this transformer");
        }

        if (image.getImageType() != ImageType.MAINTENANCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maintenance records can only be created for MAINTENANCE images");
        }

        Inspection inspection = null;
        if (body.inspectionId() != null) {
            inspection = inspectionRepository.findById(body.inspectionId())
                    .orElseThrow(() -> new NotFoundException("Inspection " + body.inspectionId() + " not found"));
            if (inspection.getTransformer() == null ||
                    !Objects.equals(inspection.getTransformer().getId(), id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Inspection does not belong to this transformer");
            }
        } else if (image.getInspection() != null) {
            inspection = image.getInspection();
        }

        if (maintenanceRecordRepository.findByMaintenanceImageId(image.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A maintenance record already exists for this image. Use PUT to update.");
        }

        LocalDateTime inspectionTimestamp = body.inspectionTimestamp();
        if (inspectionTimestamp == null) {
            if (inspection != null && inspection.getCreatedAt() != null) {
                inspectionTimestamp = inspection.getCreatedAt();
            } else {
                inspectionTimestamp = image.getCreatedAt();
            }
        }

        MaintenanceRecord rec = MaintenanceRecord.builder()
                .transformer(transformer)
                .inspection(inspection)
                .maintenanceImage(image)
                .inspectionTimestamp(inspectionTimestamp)
                .inspectorName(body.inspectorName())
                .status(body.status())
                .electricalReadings(
                        body.electricalReadings() == null
                                ? new HashMap<>()
                                : new HashMap<>(body.electricalReadings()))
                .recommendedAction(body.recommendedAction())
                .additionalRemarks(body.additionalRemarks())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        maintenanceRecordRepository.save(rec);

        return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceRecordDTO.fromEntity(rec));
    }

    @PutMapping("/maintenance-records/{recordId}")
    public ResponseEntity<MaintenanceRecordDTO> updateMaintenanceRecord(
            @PathVariable Long recordId,
            @RequestBody @Valid UpdateMaintenanceRecordDTO body) {

        SessionUser current = CurrentUserHolder.get();
        if (current == null || current.occupation() != UserOccupation.MAINTENANCE_ENGINEER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only maintenance engineers can modify maintenance records"
            );
        }

        if (!recordId.equals(body.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Record ID in path does not match id in body");
        }

        MaintenanceRecord rec = maintenanceRecordRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Maintenance record " + recordId + " not found"));

        if (body.inspectionId() != null) {
            Inspection inspection = inspectionRepository.findById(body.inspectionId())
                    .orElseThrow(() -> new NotFoundException("Inspection " + body.inspectionId() + " not found"));
            if (inspection.getTransformer() != null &&
                    !Objects.equals(inspection.getTransformer().getId(), rec.getTransformer().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Inspection does not belong to the same transformer as this record");
            }
            rec.setInspection(inspection);
        }

        if (body.inspectionTimestamp() != null) {
            rec.setInspectionTimestamp(body.inspectionTimestamp());
        }

        if (body.inspectorName() != null) {
            rec.setInspectorName(body.inspectorName());
        }

        if (body.status() != null) {
            rec.setStatus(body.status());
        }

        if (body.electricalReadings() != null) {
            rec.setElectricalReadings(new HashMap<>(body.electricalReadings()));
        }

        if (body.recommendedAction() != null) {
            rec.setRecommendedAction(body.recommendedAction());
        }

        if (body.additionalRemarks() != null) {
            rec.setAdditionalRemarks(body.additionalRemarks());
        }

        rec.setUpdatedAt(LocalDateTime.now());
        maintenanceRecordRepository.save(rec);

        return ResponseEntity.ok(MaintenanceRecordDTO.fromEntity(rec));
    }

    @GetMapping("/{id}/maintenance-records")
    public List<MaintenanceRecordDTO> listMaintenanceRecords(@PathVariable Long id) {
        if (!transformers.existsById(id)) {
            throw new NotFoundException("Transformer " + id + " not found");
        }

        return maintenanceRecordRepository
                .findByTransformerIdOrderByInspectionTimestampDesc(id)
                .stream()
                .map(MaintenanceRecordDTO::fromEntity)
                .toList();
    }

    @GetMapping("/maintenance-records/{recordId}")
    public ResponseEntity<MaintenanceRecordDTO> getMaintenanceRecord(@PathVariable Long recordId) {
        return maintenanceRecordRepository.findById(recordId)
                .map(rec -> ResponseEntity.ok(MaintenanceRecordDTO.fromEntity(rec)))
                .orElse(ResponseEntity.notFound().build());
    }
}
