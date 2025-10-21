# Training with Deleted Results Fix

## Overview

Updated the classification training service to exclude deleted anomaly results when sending data to the classification server for training.

## Problem

Previously, when training the classification model via `POST /api/transformers/{id}/train`, the system would send ALL fault regions to the classification server, including those that users had marked as deleted (soft-deleted with `is_deleted = true`).

This caused issues because:
- Deleted results represent false positives or incorrectly detected anomalies
- Including them in training would teach the model incorrect patterns
- User corrections were not being respected in the training process

## Solution

Modified `ClassificationTrainingService.trainModel()` method to filter out deleted regions before sending data to the classification server.

### Changes Made

**File:** `src/main/java/com/example/transformer/service/ClassificationTrainingService.java`

**Before:**
```java
List<FaultRegion> faultRegions = faultRegionRepository.findByImageIdOrderByRegionIdAsc(maintenanceImageId);

if (faultRegions.isEmpty()) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "No anomaly detection results found for maintenance image " + maintenanceImageId);
}

String anomalyResultsJson = createAnomalyResultsJson(faultRegions);
```

**After:**
```java
List<FaultRegion> faultRegions = faultRegionRepository.findByImageIdOrderByRegionIdAsc(maintenanceImageId);

if (faultRegions.isEmpty()) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "No anomaly detection results found for maintenance image " + maintenanceImageId);
}

// Filter out deleted regions - don't send deleted results to classification server
List<FaultRegion> activeFaultRegions = faultRegions.stream()
        .filter(region -> region.getIsDeleted() == null || !region.getIsDeleted())
        .toList();

if (activeFaultRegions.isEmpty()) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "No active (non-deleted) anomaly detection results found for maintenance image " + maintenanceImageId);
}

String anomalyResultsJson = createAnomalyResultsJson(activeFaultRegions);
```

## Behavior

### Filter Logic
Only fault regions where:
- `is_deleted` is `null` (never deleted), OR
- `is_deleted` is `false` (explicitly marked as not deleted)

are sent to the classification server for training.

### Error Handling
If all fault regions for a maintenance image have been deleted, the training endpoint will return:
- **Status:** `400 Bad Request`
- **Message:** `"No active (non-deleted) anomaly detection results found for maintenance image {imageId}"`

This prevents training with no valid data.

## Impact on Training Workflow

### Before This Fix
1. User uploads maintenance image → anomalies detected
2. User reviews and marks false positives as deleted
3. User trains model → **PROBLEM:** deleted (incorrect) results were included in training
4. Model learns from both correct and incorrect data

### After This Fix
1. User uploads maintenance image → anomalies detected
2. User reviews and marks false positives as deleted
3. User trains model → **FIXED:** only active (non-deleted) results are sent
4. Model learns only from correct, validated data

## Testing

### Test Case 1: Training with Active Results
```bash
# Upload maintenance image
POST /api/transformers/1/images
# Returns image with anomaly results

# Delete a false positive
DELETE /api/transformers/images/{imageId}/errors/{errorId}

# Train model (should only use non-deleted results)
POST /api/transformers/1/train
{
  "transformerId": 1,
  "baselineImageId": 100,
  "maintenanceImageId": 101
}
```

**Expected:** Training succeeds with only active fault regions sent to classification server.

### Test Case 2: Training with All Deleted Results
```bash
# Delete all fault regions for an image

# Attempt to train
POST /api/transformers/1/train
```

**Expected:** Returns `400 Bad Request` with message about no active results.

## Related Endpoints

This fix affects the following endpoint:
- `POST /api/transformers/{id}/train` - Train classification model

The following endpoints are NOT affected (they still return deleted results when requested):
- `GET /api/transformers/images/{imageId}/errors?includeDeleted=true` - Returns all errors including deleted
- `GET /api/transformers/images/{imageId}/anomaly-comparison` - Returns comparison including deleted items for audit

## Benefits

1. **Improved Model Accuracy:** Training data now only includes validated, correct anomaly detections
2. **User Corrections Respected:** User feedback (deletions) are incorporated into training
3. **Better False Positive Handling:** Deleted false positives won't be used to train the model
4. **Quality Control:** Ensures only quality data is used for model improvement

## Notes

- This is a filtering operation - it does NOT modify the database
- Deleted results remain in the database for audit purposes
- The comparison endpoint still shows deleted results for transparency
- This change only affects data sent to the classification server for training

