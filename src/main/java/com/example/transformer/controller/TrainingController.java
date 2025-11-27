package com.example.transformer.controller;

import com.example.transformer.dto.TrainModelRequestDTO;
import com.example.transformer.dto.TrainModelResponseDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.repository.TransformerRepository;
import com.example.transformer.service.ClassificationTrainingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/transformers")
public class TrainingController {

    private final TransformerRepository transformers;
    private final ClassificationTrainingService classificationTrainingService;

    public TrainingController(TransformerRepository transformers,
                              ClassificationTrainingService classificationTrainingService) {
        this.transformers = transformers;
        this.classificationTrainingService = classificationTrainingService;
    }

    @PostMapping("/{id}/train")
    public ResponseEntity<TrainModelResponseDTO> trainModel(
            @PathVariable("id") Long transformerId,
            @RequestBody @Valid TrainModelRequestDTO request) {

        if (!transformerId.equals(request.transformerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transformer ID in path does not match transformer ID in request body");
        }

        if (!transformers.existsById(transformerId)) {
            throw new NotFoundException("Transformer " + transformerId + " not found");
        }

        TrainModelResponseDTO response = classificationTrainingService.trainModel(
                transformerId,
                request.baselineImageId(),
                request.maintenanceImageId());

        return ResponseEntity.ok(response);
    }
}
