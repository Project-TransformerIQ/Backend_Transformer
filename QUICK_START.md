# Quick Start Guide - Classification Training Feature

## Summary

This guide helps you quickly set up and test the new classification training feature.

## What Was Implemented

✅ **New Endpoint**: `POST /api/transformers/{id}/train`  
✅ **Service Layer**: `ClassificationTrainingService` handles all training logic  
✅ **DTOs**: Request/Response objects for training  
✅ **Configuration Management**: Automatic config updates after training  
✅ **Mock Server**: Test classification server included  
✅ **Documentation**: Complete API and usage documentation

## Quick Setup (5 minutes)

### Step 1: Update Configuration

The configuration uses the existing Flask API:

```yaml
anomaly:
  detection:
    api:
      url: http://localhost:5000
```

### Step 2: Start Flask API or Mock Server

**Option A - Use Mock Server (for testing):**

```bash
# Stop your actual Flask API first (both use port 5000)

# Install dependencies (if needed)
pip install flask flask-cors

# Start the mock server
python mock_classification_server.py
```

**Option B - Use Your Actual Flask API:**
Make sure your Flask API has the `/update-config` endpoint implemented and is running on port 5000.

The server will run on `http://localhost:5000`

### Step 3: Start Backend

```bash
# Build and run Spring Boot
./mvnw spring-boot:run
```

Or on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### Step 4: Test the Feature

Option A - Use the provided test script:

```powershell
.\test_training.ps1
```

Option B - Manual test with cURL:

```bash
curl -X POST http://localhost:8080/api/transformers/1/train \
  -H "Content-Type: application/json" \
  -d '{
    "transformerId": 1,
    "baselineImageId": 1,
    "maintenanceImageId": 2
  }'
```

## Prerequisites for Testing

Before running the test, ensure you have:

1. **A Transformer** in the database (ID: 1)

   ```bash
   POST /api/transformers
   {
     "transformerNo": "T001",
     "poleNo": "P001",
     "region": "North",
     "transformerType": "DISTRIBUTION"
   }
   ```

2. **An Inspection** for the transformer

   ```bash
   POST /api/transformers/1/inspections
   {
     "title": "Monthly Inspection",
     "inspector": "John Doe",
     "status": "OPEN"
   }
   ```

3. **A Baseline Image** (ID: 1)

   ```bash
   POST /api/transformers/1/images
   - file: [thermal image]
   - meta: {
       "imageType": "BASELINE",
       "envCondition": {"weather": "SUNNY"},
       "uploader": "inspector1"
     }
   ```

4. **A Maintenance Image** (ID: 2) - This automatically triggers anomaly detection
   ```bash
   POST /api/transformers/1/images?inspectionId=1
   - file: [thermal image]
   - meta: {
       "imageType": "MAINTENANCE",
       "uploader": "inspector1"
     }
   ```

## Expected Flow

1. **Frontend**: User clicks "Train" button
2. **Backend**: Receives training request
3. **Backend**: Collects:
   - Baseline image file
   - Maintenance image file
   - Current configuration
   - Anomaly detection results
4. **Backend**: Sends to classification server
5. **Classification Server**: Processes and returns updated config
6. **Backend**: Saves new configuration to database
7. **Backend**: Returns success response
8. **Frontend**: Shows success message with new config info

## Verify Results

### Check Database

```sql
-- View all configurations
SELECT id, config_name, is_active, created_at
FROM anomaly_detection_config
ORDER BY created_at DESC;

-- View active configuration
SELECT * FROM anomaly_detection_config
WHERE is_active = true;
```

### Check Response

Successful training returns:

```json
{
  "status": "success",
  "message": "Model trained successfully and configuration updated",
  "configId": 5,
  "configName": "default_trained_2025-10-20T14:30:15",
  "trainedAt": "2025-10-20T14:30:15.123"
}
```

## Frontend Integration

Add this to your frontend code:

```javascript
// Training function
async function trainModel(transformerId, baselineImageId, maintenanceImageId) {
  try {
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
      const error = await response.json();
      throw new Error(error.message || "Training failed");
    }

    const result = await response.json();

    // Show success message
    console.log("Training successful!");
    console.log("New config:", result.configName);
    console.log("Config ID:", result.configId);

    return result;
  } catch (error) {
    console.error("Training error:", error);
    throw error;
  }
}

// Button click handler
document.getElementById("train-button").addEventListener("click", async () => {
  const transformerId = 1; // Get from your app state
  const baselineImageId = 1; // Get from selected baseline image
  const maintenanceImageId = 2; // Get from selected maintenance image

  try {
    const result = await trainModel(
      transformerId,
      baselineImageId,
      maintenanceImageId
    );
    alert(`Training successful! New configuration: ${result.configName}`);
  } catch (error) {
    alert(`Training failed: ${error.message}`);
  }
});
```

## Troubleshooting

### Error: "Classification server not responding"

- ✓ Check if Flask API is running on port 5000
- ✓ Verify `/update-config` endpoint exists in Flask API
- ✓ Check `anomaly.detection.api.url` in application.yml

### Error: "No anomaly detection results found"

- ✓ Ensure maintenance image has been processed
- ✓ Check database: `SELECT * FROM fault_regions WHERE image_id = ?`
- ✓ Verify anomaly detection ran during image upload

### Error: "Image does not belong to transformer"

- ✓ Verify image IDs are correct
- ✓ Check image ownership: `SELECT * FROM transformer_images WHERE id = ?`

### Error: "Transformer not found"

- ✓ Create a transformer first
- ✓ Use correct transformer ID

## Production Deployment

For production:

1. **Update Configuration**

   ```yaml
   anomaly:
     detection:
       api:
         url: https://your-flask-api.com
   ```

2. **Update Flask API**

   - Implement the `/update-config` endpoint
   - Ensure it accepts baseline_image, maintenance_image, config, and anomaly_results
   - Return response in expected format (see UPDATED_IMPLEMENTATION.md)

3. **Security**

   - Add authentication for training endpoint
   - Implement rate limiting
   - Add user permission checks

4. **Monitoring**
   - Log training requests and results
   - Monitor configuration changes
   - Track training success/failure rates

## Files Reference

- **Service**: `src/main/java/com/example/transformer/service/ClassificationTrainingService.java`
- **Controller**: `src/main/java/com/example/transformer/controller/TransformerController.java`
- **DTOs**: `src/main/java/com/example/transformer/dto/TrainModel*.java`
- **Config**: `src/main/resources/application-*.yml`
- **Docs**: `CLASSIFICATION_TRAINING_README.md`
- **Test**: `test_training.ps1`
- **Mock Server**: `mock_classification_server.py`

## Need Help?

- 📖 Full documentation: `CLASSIFICATION_TRAINING_README.md`
- 📋 Implementation details: `IMPLEMENTATION_SUMMARY.md`
- 🔧 Test script: `test_training.ps1`
- 🖥️ Mock server: `mock_classification_server.py`

## Success Criteria

✅ Mock classification server starts without errors  
✅ Training endpoint responds (200 OK)  
✅ New configuration created in database  
✅ New configuration marked as active  
✅ Previous configuration deactivated  
✅ Response contains new config ID and name

---

**Ready to test!**

1. Stop your Flask API (if running)
2. Run `python mock_classification_server.py`
3. Run `.\test_training.ps1` to test

**OR** implement `/update-config` in your Flask API and test with it directly!
