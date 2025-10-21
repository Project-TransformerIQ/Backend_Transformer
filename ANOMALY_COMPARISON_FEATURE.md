# Anomaly Results Storage and Comparison Feature

## Overview

This feature implements a system to store the original anomaly detection results received from the classification server and provides an endpoint to download both the original and edited anomaly results for comparison.

## Implementation Details

### 1. Database Schema

A new table `original_anomaly_results` has been created to store the raw JSON response from the classification server:

```sql
CREATE TABLE IF NOT EXISTS original_anomaly_results (
    id BIGSERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL UNIQUE,
    anomaly_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (image_id) REFERENCES transformer_images(id) ON DELETE CASCADE
);
```

### 2. New Entity

**OriginalAnomalyResult.java**
- Stores the original anomaly detection results as JSON text
- One-to-one relationship with TransformerImage
- Automatically captures timestamp when saved

### 3. Repository

**OriginalAnomalyResultRepository.java**
- `findByImageId(Long imageId)` - Retrieves original results for a specific image
- `existsByImageId(Long imageId)` - Checks if original results exist

### 4. Workflow

#### When Maintenance Image is Uploaded:

1. Image is uploaded via `POST /api/transformers/{id}/images`
2. Anomaly detection is performed by calling the classification server
3. Results are parsed and saved to database tables:
   - `fault_regions` - Detected anomaly regions
   - `display_metadata` - Display information
   - **`original_anomaly_results`** - **NEW**: Raw JSON from classification server
4. Response includes anomaly detection results

#### When Downloading Comparison Data:

Call `GET /api/transformers/images/{imageId}/anomaly-comparison`

Response includes:
- **imageInfo**: Image metadata (filename, upload time, uploader, inspection details)
- **originalResults**: Raw data as received from classification server
  - `receivedAt`: Timestamp when results were received
  - `data`: Original JSON response
- **currentResults**: Current edited state
  - `fault_regions`: Current fault regions (including user edits)
  - `display_metadata`: Display metadata
  - `editSummary`: Statistics about edits
    - `totalRegions`: Total number of regions
    - `manuallyAdded`: Count of manually added regions
    - `deleted`: Count of deleted regions
    - `modified`: Count of modified regions
- **generatedAt**: Timestamp when comparison was generated

## API Endpoints

### New Endpoint

#### Download Anomaly Comparison
```
GET /api/transformers/images/{imageId}/anomaly-comparison
```

**Path Parameters:**
- `imageId` (Long) - ID of the maintenance image

**Response:** 200 OK
```json
{
  "imageInfo": {
    "imageId": 123,
    "filename": "maintenance_image.jpg",
    "uploadedAt": "2025-10-20T10:30:00",
    "uploader": "John Doe",
    "inspectionId": 45,
    "inspectionTitle": "Monthly Inspection"
  },
  "originalResults": {
    "receivedAt": "2025-10-20T10:30:15",
    "data": {
      "fault_regions": [...],
      "display_metadata": {...}
    }
  },
  "currentResults": {
    "fault_regions": [...],
    "display_metadata": {...},
    "editSummary": {
      "totalRegions": 5,
      "manuallyAdded": 1,
      "deleted": 1,
      "modified": 2
    }
  },
  "generatedAt": "2025-10-21T14:20:00"
}
```

**Response Headers:**
- `Content-Type: application/json`
- `Content-Disposition: attachment; filename="anomaly-comparison-image-{imageId}.json"`

**Error Responses:**
- `404 Not Found` - Image not found
- `400 Bad Request` - Image is not a maintenance image

## Use Cases

### 1. Audit Trail
Track what was originally detected by the AI vs what was manually corrected by users.

### 2. Model Performance Analysis
Compare original detection results with user-corrected data to improve the classification model.

### 3. Training Data Preparation
Use the comparison data to prepare training datasets for model retraining.

### 4. Quality Assurance
Verify that user edits are appropriate and identify patterns in manual corrections.

### 5. Reporting
Generate reports showing the accuracy of automated detection vs manual inspection.

## Example Usage

### Upload Maintenance Image
```bash
curl -X POST "http://localhost:8080/api/transformers/1/images" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@maintenance.jpg" \
  -F 'meta={"imageType":"MAINTENANCE","inspectionId":45,"uploader":"John Doe"}'
```

### Download Comparison Data
```bash
curl -X GET "http://localhost:8080/api/transformers/images/123/anomaly-comparison" \
  -H "Accept: application/json" \
  -o anomaly-comparison.json
```

## Database Migration

The database migration file `V12__create_original_anomaly_results.sql` will be automatically executed by Flyway when the application starts.

**Migration Version:** V12

## Notes

- Original anomaly results are only stored for maintenance images
- The original JSON is stored as-is, preserving the exact response from the classification server
- If anomaly detection fails, no original results will be stored
- The comparison endpoint returns `null` for `originalResults` if no original data exists
- The feature maintains backward compatibility with existing endpoints

## Future Enhancements

1. Add endpoint to re-run anomaly detection and compare with previous results
2. Add statistics API to show overall correction patterns
3. Add bulk export for multiple images
4. Add filtering options (e.g., only show differences)
5. Add visual diff highlighting in the response

