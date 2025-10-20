# Configuration Structure Update Summary

## Overview

Updated the anomaly detection configuration structure from a 13-parameter SSIM/MSE-based system to a 41-parameter thermal image analysis system matching the Python Flask API requirements.

## Date

Updated: ${new Date().toISOString()}

## Changes Made

### 1. Entity Model Update

**File**: `src/main/java/com/example/transformer/model/AnomalyDetectionConfig.java`

#### Removed Parameters (Old Structure - 13 parameters):

- `ssimWeight`, `ssimThreshold`
- `mseWeight`, `mseThreshold`
- `histogramWeight`, `histogramThreshold`
- `combinedThreshold`
- `resizeWidth`, `resizeHeight`
- `blurKernelSize`
- `minContourArea`
- `dilationIterations`, `erosionIterations`

#### Added Parameters (New Structure - 41 parameters):

1. **Delta Detection**:

   - `deltaKSigma` (0.28)
   - `deltaAbsMin` (5)

2. **Blob Processing**:

   - `minBlobAreaPx` (24)
   - `openIters` (2)
   - `dilateIters` (8)
   - `keepComponentMinRatio` (0.3)

3. **Fault Detection**:

   - `faultRedRatio` (0.00007)
   - `faultRedMinPixels` (5)
   - `potentialYellowRatio` (0.0002)
   - `fullwireHotFraction` (0.4)

4. **Region Analysis**:

   - `elongatedAspectRatio` (3.0)
   - `mergeCloseFrac` (0.06)
   - `minClusterAreaPx` (400)

5. **Sidebar Detection**:

   - `sidebarSearchFrac` (0.35)
   - `sidebarMinWidthFrac` (0.03)
   - `sidebarMaxWidthFrac` (0.09)
   - `sidebarMinValidFrac` (0.6)
   - `sidebarHueSpanDeg` (25)
   - `sidebarMarginPx` (10)
   - `textBottomBandFrac` (0.1)

6. **Overlay Masking**:

   - `maskTopLeftOverlay` (true)
   - `topLeftBox` ("0,0,200,230") - stored as comma-separated string

7. **Histogram Analysis**:

   - `hBins` (256)
   - `histDistanceMin` (0.03)

8. **Background Detection - Red**:

   - `redBgRatioMinIncrease` (0.001)
   - `redBgMinAbs` (0.001)

9. **ROI Filters**:

   - `roiSMin` (25)
   - `roiVMin` (50)

10. **Background Detection - Blue**:

    - `blueHLo` (90)
    - `blueHHi` (128)
    - `blueSMin` (40)
    - `blueVMin` (40)

11. **Background Detection - Black**:

    - `blackVHi` (40)

12. **Background Detection - White**:
    - `whiteBgSMax` (30)
    - `whiteBgVMin` (170)
    - `whiteBgExcludeNearWarmPx` (4)
    - `whiteBgColumnFrac` (0.25)
    - `whiteBgRowFrac` (0.15)

### 2. Database Migration

**File**: `src/main/resources/db/migration/V11__update_anomaly_detection_config.sql`

- Drops all 13 old configuration columns
- Adds all 41 new configuration columns with default values
- Preserves existing records (id, config_name, description, is_active, created_at, updated_at)

**Important**: Run this migration before starting the application with the new code.

### 3. Service Layer Updates

#### AnomalyDetectionService.java

**File**: `src/main/java/com/example/transformer/service/AnomalyDetectionService.java`

**Changes**:

- Updated `createConfigJson()` method to generate JSON with all 41 parameters
- Converts `topLeftBox` from string "x1,y1,x2,y2" to List<Double> for JSON
- Matches exact Python dict structure expected by Flask API

**Added Imports**:

- `java.util.ArrayList`
- `java.util.List`

#### ClassificationTrainingService.java

**File**: `src/main/java/com/example/transformer/service/ClassificationTrainingService.java`

**Changes**:

- Updated `createConfigJson()` method to generate JSON with all 41 parameters
- Updated `updateConfigurationFromTrainingResponse()` method to parse all 41 parameters
- Converts `topLeftBox` bidirectionally:
  - String → List<Double> when sending to Flask API
  - List<Double> → String when receiving from Flask API

**Added Imports**:

- `java.util.ArrayList`

## API Integration

### Flask API Endpoint

- **URL**: `http://localhost:5000/update-config`
- **Method**: POST
- **Content-Type**: multipart/form-data

### Request Format

```
POST /update-config
Content-Type: multipart/form-data

Parts:
- baseline_image: image file
- maintenance_image: image file
- config: JSON file with 41 parameters
- anomaly_results: JSON file with detected anomalies
```

### Configuration JSON Structure

```json
{
  "delta_k_sigma": 0.28,
  "delta_abs_min": 5,
  "min_blob_area_px": 24,
  "open_iters": 2,
  "dilate_iters": 8,
  "keep_component_min_ratio": 0.3,
  "fault_red_ratio": 0.00007,
  "fault_red_min_pixels": 5,
  "potential_yellow_ratio": 0.0002,
  "fullwire_hot_fraction": 0.4,
  "elongated_aspect_ratio": 3.0,
  "merge_close_frac": 0.06,
  "min_cluster_area_px": 400,
  "sidebar_search_frac": 0.35,
  "sidebar_min_width_frac": 0.03,
  "sidebar_max_width_frac": 0.09,
  "sidebar_min_valid_frac": 0.6,
  "sidebar_hue_span_deg": 25,
  "sidebar_margin_px": 10,
  "text_bottom_band_frac": 0.1,
  "mask_top_left_overlay": true,
  "top_left_box": [0.0, 0.0, 0.5, 0.12],
  "h_bins": 256,
  "hist_distance_min": 0.03,
  "red_bg_ratio_min_increase": 0.001,
  "red_bg_min_abs": 0.001,
  "roi_s_min": 25,
  "roi_v_min": 50,
  "blue_h_lo": 90,
  "blue_h_hi": 128,
  "blue_s_min": 40,
  "blue_v_min": 40,
  "black_v_hi": 40,
  "white_bg_s_max": 30,
  "white_bg_v_min": 170,
  "white_bg_exclude_near_warm_px": 4,
  "white_bg_column_frac": 0.25,
  "white_bg_row_frac": 0.15
}
```

### Response Format

```json
{
  "status": "success",
  "message": "Configuration updated successfully",
  "updated_config": {
    // All 41 parameters with new values
  }
}
```

## Database Schema

### Table: anomaly_detection_config

**Columns**:

- `id` BIGSERIAL PRIMARY KEY
- `config_name` VARCHAR(255)
- `description` TEXT
- `is_active` BOOLEAN
- `created_at` TIMESTAMP
- `updated_at` TIMESTAMP
- All 41 configuration parameters (see above)

## Testing Instructions

### 1. Run Database Migration

```bash
# The migration will run automatically on application startup with Flyway
# Or manually run:
./mvnw flyway:migrate
```

### 2. Verify Migration

```sql
-- Check table structure
\d anomaly_detection_config

-- Verify columns exist
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'anomaly_detection_config';
```

### 3. Test Training Endpoint

```powershell
# Use the test script
.\test_training.ps1

# Or manually with curl:
curl -X POST http://localhost:8080/api/transformers/1/train `
  -H "Content-Type: application/json" `
  -d '{"transformerId": 1, "baselineImageId": 123, "maintenanceImageId": 456}'
```

### 4. Verify Configuration Save

```sql
-- Check saved configurations
SELECT id, config_name, delta_k_sigma, fault_red_ratio, elongated_aspect_ratio
FROM anomaly_detection_config
ORDER BY created_at DESC;
```

## Parameter Name Mapping

| Python (snake_case) | Java (camelCase) | Database (snake_case) |
| ------------------- | ---------------- | --------------------- |
| delta_k_sigma       | deltaKSigma      | delta_k_sigma         |
| delta_abs_min       | deltaAbsMin      | delta_abs_min         |
| min_blob_area_px    | minBlobAreaPx    | min_blob_area_px      |
| fault_red_ratio     | faultRedRatio    | fault_red_ratio       |
| top_left_box        | topLeftBox       | top_left_box          |
| ...                 | ...              | ...                   |

## Special Handling

### top_left_box Field

- **Python/JSON Format**: Tuple of 4 doubles `(x1, y1, x2, y2)` - normalized coordinates (0.0-1.0)
- **Java Format**: String `"x1,y1,x2,y2"` - comma-separated normalized coordinates
- **Database Format**: VARCHAR(255) with comma-separated values
- **Coordinates**: Normalized fractions of image dimensions (0.0 = 0%, 0.5 = 50%, 1.0 = 100%)

**Example**: `"0.0,0.0,0.5,0.12"` represents top-left corner (0%,0%) to (50% width, 12% height)

**Conversion Logic**:

```java
// String to List (for sending to Flask API)
String[] parts = topLeftBoxStr.split(",");
List<Double> topLeftBox = new ArrayList<>();
for (String part : parts) {
    topLeftBox.add(Double.parseDouble(part.trim()));
}

// List to String (for saving to database)
StringBuilder sb = new StringBuilder();
for (int i = 0; i < topLeftBoxNode.size(); i++) {
    if (i > 0) sb.append(",");
    sb.append(topLeftBoxNode.get(i).asDouble());
}
```

## Rollback Plan

If issues occur, rollback using:

### 1. Code Rollback

```bash
git revert <commit-hash>
```

### 2. Database Rollback

```sql
-- Restore old columns (if backup exists)
ALTER TABLE anomaly_detection_config
ADD COLUMN ssim_weight DOUBLE PRECISION,
ADD COLUMN mse_weight DOUBLE PRECISION,
... -- etc for all old columns

-- Remove new columns
ALTER TABLE anomaly_detection_config
DROP COLUMN delta_k_sigma,
DROP COLUMN delta_abs_min,
... -- etc for all new columns
```

## Known Issues & Limitations

1. **Existing Data**: Any existing configuration records will have NULL values for new columns after migration (default values from migration will apply to NEW records only)
2. **Training Required**: The training endpoint must be called to populate configurations with ML-optimized values
3. **top_left_box Validation**: No built-in validation for the format - ensure it's always 4 comma-separated normalized coordinates (0.0-1.0 range)

## Next Steps

1. ✅ Update entity model
2. ✅ Create database migration
3. ✅ Update service layer
4. ✅ Update training endpoint logic
5. ⏳ Test with Flask API
6. ⏳ Verify configuration persistence
7. ⏳ Performance testing with real images

## Related Documentation

- [Classification Training README](./CLASSIFICATION_TRAINING_README.md)
- [Quick Start Guide](./QUICK_START.md)
- [Implementation Summary](./IMPLEMENTATION_SUMMARY.md)

## Contact

For questions or issues related to this update, please contact the development team.
