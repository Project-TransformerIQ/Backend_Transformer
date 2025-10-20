# Classification Training Implementation Summary

## Overview

This implementation adds a complete classification training feature to the Transformer Thermal Inspection backend. When the user clicks the "Train" button in the frontend, the backend sends baseline images, maintenance images, current configuration parameters, and anomaly detection results to a classification server. The classification server returns an updated configuration that is automatically saved to the database and activated.

## Files Created

### 1. DTOs (Data Transfer Objects)

- **`TrainModelRequestDTO.java`**: Request DTO for training endpoint

  - Fields: `transformerId`, `baselineImageId`, `maintenanceImageId`
  - Validation: All fields required

- **`TrainModelResponseDTO.java`**: Response DTO for training endpoint
  - Fields: `status`, `message`, `configId`, `configName`, `trainedAt`

### 2. Service Layer

- **`ClassificationTrainingService.java`**: Core service handling training workflow
  - Method: `trainModel()` - Main training orchestration
  - Method: `updateConfigurationFromTrainingResponse()` - Parse and save updated config
  - Method: `getActiveConfig()` - Get current active configuration
  - Method: `createConfigJson()` - Convert config object to JSON
  - Method: `createAnomalyResultsJson()` - Convert fault regions to JSON

### 3. Controller

- **Modified `TransformerController.java`**:
  - Added `ClassificationTrainingService` dependency
  - Added endpoint: `POST /api/transformers/{id}/train`
  - Added imports for training DTOs

### 4. Configuration

- **Modified `application-prod.yml`**: Added classification server URL
- **Modified `aplication-dev.yml`**: Added classification server URL

### 5. Documentation

- **`CLASSIFICATION_TRAINING_README.md`**: Complete documentation of the feature
- **`test_training.ps1`**: PowerShell test script
- **`mock_classification_server.py`**: Mock Flask server for testing

## API Endpoint

### POST /api/transformers/{id}/train

**Request:**

```json
{
  "transformerId": 1,
  "baselineImageId": 123,
  "maintenanceImageId": 456
}
```

**Response:**

```json
{
  "status": "success",
  "message": "Model trained successfully and configuration updated",
  "configId": 5,
  "configName": "default_trained_2025-10-20T14:30:15",
  "trainedAt": "2025-10-20T14:30:15.123"
}
```

## What Gets Sent to Classification Server

The service sends a multipart POST request to `{classification.api.url}/train` with:

1. **baseline_image** (file): The baseline thermal image
2. **maintenance_image** (file): The maintenance thermal image
3. **config** (JSON file): Current anomaly detection configuration
4. **anomaly_results** (JSON file): Detected fault regions from anomaly detection

## Expected Response Format

The classification server should respond with:

```json
{
  "status": "success",
  "message": "Training completed",
  "updated_config": {
    "ssim": { "weight": 0.6, "threshold": 0.9 },
    "mse": { "weight": 0.25, "threshold": 950.0 },
    "histogram": { "weight": 0.15, "threshold": 0.75 },
    "combined_threshold": 0.8,
    "image_processing": {
      "resize_width": 1024,
      "resize_height": 768,
      "blur_kernel_size": 3
    },
    "detection": {
      "min_contour_area": 80,
      "dilation_iterations": 3,
      "erosion_iterations": 2
    }
  }
}
```

## Configuration Management

### How It Works:

1. **Current config retrieved**: Gets the active configuration from database
2. **Data sent**: Sends images, config, and anomaly results to classification server
3. **Response parsed**: Extracts updated configuration parameters from response
4. **Current deactivated**: Sets current config's `is_active` to `false`
5. **New config created**: Creates new configuration record with:
   - Name: `{old_name}_trained_{timestamp}`
   - Updated parameters from classification server
   - `is_active = true`
6. **Saved to database**: Persists new configuration
7. **Response returned**: Returns training status and new config info

### Configuration Versioning

Each training creates a new configuration, preserving history:

```
default                           -> is_active: false (original)
default_trained_2025-10-20T10:15  -> is_active: false (1st training)
default_trained_2025-10-20T14:30  -> is_active: true  (2nd training - active)
```

## Validation

The service validates:

- ✓ Transformer exists
- ✓ Baseline image exists and belongs to transformer
- ✓ Maintenance image exists and belongs to transformer
- ✓ Anomaly detection results exist for maintenance image
- ✓ Classification server response contains updated config

## Error Handling

- **404 NOT FOUND**: Transformer or images not found
- **400 BAD REQUEST**:
  - Images don't belong to transformer
  - No anomaly results found
  - Invalid request data
- **500 INTERNAL SERVER ERROR**:
  - Classification server error
  - Invalid response format
  - Database error

## Testing

### Prerequisites:

1. Spring Boot backend running (port 8080)
2. Classification server running (port 5001)
3. Transformer with baseline and maintenance images
4. Maintenance image must have anomaly detection results

### Using Mock Server:

```bash
# Install Flask if needed
pip install flask flask-cors

# Run mock server
python mock_classification_server.py

# In another terminal, run test script
.\test_training.ps1
```

### Manual Testing with cURL:

```bash
curl -X POST http://localhost:8080/api/transformers/1/train \
  -H "Content-Type: application/json" \
  -d '{
    "transformerId": 1,
    "baselineImageId": 123,
    "maintenanceImageId": 456
  }'
```

## Configuration Properties

Add to your application configuration:

```yaml
classification:
  api:
    url: http://localhost:5001 # or your production URL
```

## Integration with Frontend

Frontend should call the endpoint when user clicks "Train" button:

```javascript
const response = await fetch(`/api/transformers/${transformerId}/train`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    transformerId,
    baselineImageId,
    maintenanceImageId,
  }),
});

const result = await response.json();
// result contains: status, message, configId, configName, trainedAt
```

## Benefits

1. **Automated**: Configuration updates automatically after training
2. **Versioned**: All configurations preserved with timestamps
3. **Safe**: Non-destructive updates (old configs kept)
4. **Integrated**: Works seamlessly with existing anomaly detection
5. **Validated**: Comprehensive input validation
6. **Documented**: Full documentation and examples provided

## Next Steps

1. Deploy the backend changes
2. Implement or deploy the classification server at the configured URL
3. Update frontend to call the training endpoint
4. Test with real thermal images
5. Monitor training results and configuration changes

## Database Impact

- No schema changes required
- Uses existing `anomaly_detection_config` table
- Creates new configuration records on each training
- Previous configurations remain in database

## Performance Considerations

- Training is synchronous (blocking)
- Consider adding async processing for production
- Classification server response time affects user experience
- Consider timeout configuration for classification server calls

## Security Considerations

- Validate user permissions before allowing training
- Consider rate limiting on training endpoint
- Validate file sizes for images sent to classification server
- Sanitize classification server responses before saving to database
