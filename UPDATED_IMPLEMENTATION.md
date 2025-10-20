# Updated Implementation - Using Existing Flask API

## Important Changes

The classification training feature now uses the **existing Flask API** at `http://localhost:5000` instead of a separate classification server.

### Key Updates

1. **Endpoint Changed**:

   - Old: `http://localhost:5001/train`
   - **New: `http://localhost:5000/update-config`**

2. **Single Server**: Both anomaly detection and classification training use the same Flask API server

3. **Configuration**: Only one URL needed in `application.yml`:
   ```yaml
   anomaly:
     detection:
       api:
         url: http://localhost:5000
   ```

## Architecture

```
Frontend (Train Button)
    ↓
Backend Training Endpoint (/api/transformers/{id}/train)
    ↓
ClassificationTrainingService
    ↓ (collects)
    ├── Baseline Image
    ├── Maintenance Image
    ├── Current Configuration
    └── Anomaly Detection Results
    ↓ (sends to)
Flask API @ http://localhost:5000/update-config
    ↓ (processes and returns)
Updated Configuration
    ↓ (saves to)
Database (new active config)
    ↓
Response to Frontend
```

## Flask API Endpoint: `/update-config`

### What Backend Sends

**HTTP Method**: `POST`  
**Content-Type**: `multipart/form-data`

**Form Data**:

- `baseline_image` (file): Baseline thermal image
- `maintenance_image` (file): Maintenance thermal image
- `config` (JSON file): Current configuration
- `anomaly_results` (JSON file): Detected fault regions

### Configuration JSON Format

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

### Anomaly Results JSON Format

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

### Expected Response from Flask API

The Flask API `/update-config` endpoint should return:

```json
{
  "status": "success",
  "message": "Configuration updated successfully",
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

## Flask API Implementation Example

Here's how to implement the `/update-config` endpoint in your Flask API:

```python
from flask import Flask, request, jsonify
import json

app = Flask(__name__)

@app.route('/update-config', methods=['POST'])
def update_config():
    """
    Train classification model and return updated configuration
    """
    try:
        # Get uploaded files
        baseline_image = request.files.get('baseline_image')
        maintenance_image = request.files.get('maintenance_image')
        config_file = request.files.get('config')
        anomaly_file = request.files.get('anomaly_results')

        # Validate all files are present
        if not all([baseline_image, maintenance_image, config_file, anomaly_file]):
            return jsonify({
                "status": "error",
                "message": "Missing required files"
            }), 400

        # Parse current configuration
        current_config = json.loads(config_file.read().decode('utf-8'))

        # Parse anomaly results
        anomaly_results = json.loads(anomaly_file.read().decode('utf-8'))
        fault_regions = anomaly_results.get('fault_regions', [])

        # TODO: Your ML training logic here
        # 1. Load and process baseline_image
        # 2. Load and process maintenance_image
        # 3. Analyze fault_regions
        # 4. Train/update your classification model
        # 5. Optimize configuration parameters

        # Generate updated configuration
        updated_config = optimize_configuration(
            current_config,
            baseline_image,
            maintenance_image,
            fault_regions
        )

        return jsonify({
            "status": "success",
            "message": f"Configuration updated successfully. Analyzed {len(fault_regions)} fault regions.",
            "updated_config": updated_config
        }), 200

    except Exception as e:
        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

def optimize_configuration(current_config, baseline, maintenance, fault_regions):
    """
    Your ML logic to optimize configuration based on training data
    """
    # Implement your optimization logic here
    # This is a placeholder that slightly adjusts the current config

    updated = current_config.copy()

    # Example: Adjust thresholds based on fault region analysis
    if len(fault_regions) > 10:
        # Many faults detected - make detection more sensitive
        updated['combined_threshold'] = max(0.65, current_config['combined_threshold'] - 0.05)
    elif len(fault_regions) < 3:
        # Few faults - make detection less sensitive
        updated['combined_threshold'] = min(0.85, current_config['combined_threshold'] + 0.05)

    return updated

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
```

## Testing

### Option 1: Test with Mock Server (for development)

**Note**: Stop your actual Flask API before running the mock server (both use port 5000)

```bash
# Stop your Flask API first!

# Run the mock server
python mock_classification_server.py
```

### Option 2: Test with Actual Flask API

```bash
# Make sure your Flask API has the /update-config endpoint implemented
python your_flask_api.py
```

### Run the Test Script

```powershell
.\test_training.ps1
```

### Manual Test with cURL

```bash
curl -X POST http://localhost:8080/api/transformers/1/train \
  -H "Content-Type: application/json" \
  -d '{
    "transformerId": 1,
    "baselineImageId": 1,
    "maintenanceImageId": 2
  }'
```

## Configuration

Your `application.yml` (both dev and prod):

```yaml
anomaly:
  detection:
    api:
      url: http://localhost:5000 # Same server for both operations
```

For production, update to your production Flask API URL:

```yaml
anomaly:
  detection:
    api:
      url: https://your-flask-api.com
```

## What Changed

| Component        | Before                        | After                                 |
| ---------------- | ----------------------------- | ------------------------------------- |
| Endpoint URL     | `http://localhost:5001/train` | `http://localhost:5000/update-config` |
| Config property  | `classification.api.url`      | `anomaly.detection.api.url` (reused)  |
| Server count     | 2 servers (ports 5000 & 5001) | 1 server (port 5000)                  |
| Service variable | `classificationApiUrl`        | `flaskApiUrl`                         |

## Benefits

✅ **Simpler Architecture**: One Flask API server handles both operations  
✅ **Easier Deployment**: No need to deploy separate classification server  
✅ **Consistent Configuration**: Single URL for all Flask API calls  
✅ **Reduced Complexity**: Fewer services to manage

## Frontend Integration (No Changes Needed)

The frontend integration remains the same:

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
// result: { status, message, configId, configName, trainedAt }
```

## Summary

- ✅ Backend updated to use `/update-config` endpoint
- ✅ Configuration simplified to use single Flask API URL
- ✅ Mock server updated to run on port 5000
- ✅ All documentation updated
- ✅ Ready to integrate with existing Flask API

**Next Step**: Implement the `/update-config` endpoint in your Flask API at `http://localhost:5000`
