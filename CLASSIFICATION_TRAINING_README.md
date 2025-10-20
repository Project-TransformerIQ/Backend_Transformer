# Classification Training Feature

## Overview

This feature allows training a classification model by sending baseline images, maintenance images, current configuration parameters, and anomaly detection results to a classification server. The classification server processes this data and returns an updated configuration that is automatically saved to the database and activated.

## Architecture

### Components

1. **ClassificationTrainingService**: Service class that handles the training workflow
2. **TrainModelRequestDTO**: Request DTO containing transformer and image IDs
3. **TrainModelResponseDTO**: Response DTO containing training status and updated config info
4. **Endpoint**: `POST /api/transformers/{id}/train`

### Workflow

```
Frontend (Train Button Click)
    ↓
Backend Training Endpoint
    ↓
ClassificationTrainingService
    ↓ (collects data)
    ├── Baseline Image
    ├── Maintenance Image
    ├── Current Configuration
    └── Anomaly Detection Results
    ↓ (sends to)
Classification Server (http://localhost:5001/train)
    ↓ (processes and returns)
Updated Configuration
    ↓ (saves to)
Database (new active config)
    ↓
Response to Frontend
```

## API Endpoints

### Train Model

**Endpoint:** `POST /api/transformers/{id}/train`

**Description:** Trains the classification model with the specified images and updates the anomaly detection configuration.

**Request Body:**

```json
{
  "transformerId": 1,
  "baselineImageId": 123,
  "maintenanceImageId": 456
}
```

**Request Parameters:**

- `transformerId` (Long, required): The ID of the transformer
- `baselineImageId` (Long, required): The ID of the baseline image to use for training
- `maintenanceImageId` (Long, required): The ID of the maintenance image to use for training

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

**HTTP Status Codes:**

- `200 OK`: Training successful, configuration updated
- `400 BAD REQUEST`: Invalid request (e.g., image doesn't belong to transformer, no anomaly results found)
- `404 NOT FOUND`: Transformer or images not found
- `500 INTERNAL SERVER ERROR`: Training failed or server error

## What Gets Sent to Classification Server

The training service sends the following data as multipart form data to the classification server:

1. **baseline_image** (File): The baseline thermal image
2. **maintenance_image** (File): The maintenance thermal image
3. **config** (JSON file): Current anomaly detection configuration
4. **anomaly_results** (JSON file): Anomaly detection results (fault regions)

### Configuration JSON Structure

```json
{
  "ssim": {
    "weight": 0.5,
    "threshold": 0.85
  },
  "mse": {
    "weight": 0.3,
    "threshold": 1000.0
  },
  "histogram": {
    "weight": 0.2,
    "threshold": 0.7
  },
  "combined_threshold": 0.75,
  "image_processing": {
    "resize_width": 800,
    "resize_height": 600,
    "blur_kernel_size": 5
  },
  "detection": {
    "min_contour_area": 100,
    "dilation_iterations": 2,
    "erosion_iterations": 1
  }
}
```

### Anomaly Results JSON Structure

```json
{
  "fault_regions": [
    {
      "id": 1,
      "type": "hotspot",
      "dominant_color": "red",
      "color_rgb": [255, 0, 0],
      "boundingBox": {
        "x": 100,
        "y": 150,
        "width": 50,
        "height": 60,
        "areaPx": 3000
      },
      "centroid": {
        "x": 125,
        "y": 180
      },
      "aspect_ratio": 0.833,
      "elongated": false,
      "connected_to_wire": true,
      "tag": "critical",
      "confidence": 0.95
    }
  ]
}
```

## Expected Response from Classification Server

The classification server should return a JSON response with the following structure:

```json
{
  "status": "success",
  "message": "Model trained successfully",
  "updated_config": {
    "ssim": {
      "weight": 0.6,
      "threshold": 0.9
    },
    "mse": {
      "weight": 0.25,
      "threshold": 950.0
    },
    "histogram": {
      "weight": 0.15,
      "threshold": 0.75
    },
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

### How Updated Configuration is Saved

1. **Deactivate Current Config**: The currently active configuration is set to `is_active = false`
2. **Create New Config**: A new configuration record is created with:
   - Name: `{old_config_name}_trained_{timestamp}`
   - Updated parameters from classification server response
   - `is_active = true`
   - Description: "Configuration updated from classification training at {timestamp}"
3. **Save to Database**: The new configuration is persisted to `anomaly_detection_config` table
4. **Return Response**: Response includes the new config ID and name

### Configuration Versioning

Each training creates a new configuration record, maintaining a history of all trained configurations. Example:

```
default                           -> is_active: false (original)
default_trained_2025-10-20T10:15  -> is_active: false (1st training)
default_trained_2025-10-20T14:30  -> is_active: true  (2nd training - current)
```

## Configuration Properties

Add these properties to your `application.yml` or `application-dev.yml`:

```yaml
# Classification Training Server URL
classification:
  api:
    url: http://localhost:5001
```

For production (`application-prod.yml`), update the URL to your production classification server.

## Usage Example

### Frontend Integration

When the user clicks the "Train" button:

```javascript
const trainModel = async (
  transformerId,
  baselineImageId,
  maintenanceImageId
) => {
  const response = await fetch(`/api/transformers/${transformerId}/train`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      transformerId,
      baselineImageId,
      maintenanceImageId,
    }),
  });

  if (!response.ok) {
    throw new Error("Training failed");
  }

  const result = await response.json();
  console.log("Training successful:", result);
  // result contains: status, message, configId, configName, trainedAt
};
```

### cURL Example

```bash
curl -X POST http://localhost:8080/api/transformers/1/train \
  -H "Content-Type: application/json" \
  -d '{
    "transformerId": 1,
    "baselineImageId": 123,
    "maintenanceImageId": 456
  }'
```

## Error Handling

The service handles various error scenarios:

1. **Image Not Found**: Returns 404 if baseline or maintenance image doesn't exist
2. **Image Mismatch**: Returns 400 if images don't belong to the specified transformer
3. **No Anomaly Results**: Returns 400 if no fault regions found for the maintenance image
4. **Classification Server Error**: Returns 500 with error message from classification server
5. **Invalid Response**: Returns 500 if classification server response doesn't contain updated config

## Database Schema

The feature uses the existing `anomaly_detection_config` table (created by migration V10):

```sql
CREATE TABLE anomaly_detection_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL UNIQUE,
    ssim_weight DOUBLE DEFAULT 0.5,
    ssim_threshold DOUBLE DEFAULT 0.85,
    mse_weight DOUBLE DEFAULT 0.3,
    mse_threshold DOUBLE DEFAULT 1000.0,
    histogram_weight DOUBLE DEFAULT 0.2,
    histogram_threshold DOUBLE DEFAULT 0.7,
    combined_threshold DOUBLE DEFAULT 0.75,
    resize_width INT DEFAULT 800,
    resize_height INT DEFAULT 600,
    blur_kernel_size INT DEFAULT 5,
    min_contour_area INT DEFAULT 100,
    dilation_iterations INT DEFAULT 2,
    erosion_iterations INT DEFAULT 1,
    is_active BOOLEAN DEFAULT FALSE,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Testing

### Prerequisites

1. Start the Spring Boot backend
2. Start the classification server on port 5001 with `/train` endpoint
3. Have at least one transformer with baseline and maintenance images
4. Ensure maintenance image has anomaly detection results (fault regions)

### Test Steps

1. Upload a baseline image to a transformer
2. Upload a maintenance image (this triggers anomaly detection automatically)
3. Call the training endpoint with the image IDs
4. Verify the response contains the new configuration details
5. Check database to confirm new configuration is active

### Mock Classification Server (for testing)

You can create a simple mock server for testing:

```python
from flask import Flask, request, jsonify
import random

app = Flask(__name__)

@app.route('/train', methods=['POST'])
def train():
    # Simulate processing
    baseline = request.files.get('baseline_image')
    maintenance = request.files.get('maintenance_image')
    config = request.files.get('config')
    anomaly = request.files.get('anomaly_results')

    # Return updated configuration with slightly adjusted values
    return jsonify({
        "status": "success",
        "message": "Model trained successfully with mock server",
        "updated_config": {
            "ssim": {
                "weight": round(random.uniform(0.4, 0.7), 2),
                "threshold": round(random.uniform(0.80, 0.95), 2)
            },
            "mse": {
                "weight": round(random.uniform(0.2, 0.4), 2),
                "threshold": round(random.uniform(800, 1200), 1)
            },
            "histogram": {
                "weight": round(random.uniform(0.1, 0.3), 2),
                "threshold": round(random.uniform(0.65, 0.80), 2)
            },
            "combined_threshold": round(random.uniform(0.70, 0.85), 2),
            "image_processing": {
                "resize_width": random.choice([800, 1024, 1280]),
                "resize_height": random.choice([600, 768, 960]),
                "blur_kernel_size": random.choice([3, 5, 7])
            },
            "detection": {
                "min_contour_area": random.randint(50, 150),
                "dilation_iterations": random.randint(1, 4),
                "erosion_iterations": random.randint(1, 3)
            }
        }
    })

if __name__ == '__main__':
    app.run(port=5001, debug=True)
```

## Benefits

1. **Automated Configuration Updates**: Configuration is automatically updated based on training results
2. **Configuration History**: All configurations are preserved with timestamps
3. **Non-Destructive**: Previous configurations are kept, not overwritten
4. **Seamless Integration**: Works with existing anomaly detection workflow
5. **Validation**: Comprehensive validation of inputs before sending to classification server
6. **Error Handling**: Robust error handling and meaningful error messages

## Future Enhancements

1. Add ability to select specific configuration for training
2. Add batch training with multiple image pairs
3. Add training history tracking
4. Add configuration comparison tools
5. Add rollback capability to previous configurations
6. Add training metrics and performance tracking
