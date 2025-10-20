# Quick Reference: 41 Configuration Parameters

## Parameter Categories & Defaults

### 1. Delta Detection (2 params)

```java
deltaKSigma = 0.28              // Kernel sigma for delta detection
deltaAbsMin = 5                 // Minimum absolute delta value
```

### 2. Blob Processing (5 params)

```java
minBlobAreaPx = 24              // Minimum blob area in pixels
openIters = 2                   // Morphological opening iterations
dilateIters = 8                 // Dilation iterations
keepComponentMinRatio = 0.3     // Minimum component size ratio to keep
```

### 3. Fault Detection (4 params)

```java
faultRedRatio = 0.00007         // Red pixel ratio for fault detection
faultRedMinPixels = 5           // Minimum red pixels for fault
potentialYellowRatio = 0.0002   // Yellow pixel ratio threshold
fullwireHotFraction = 0.4       // Full wire hotspot fraction
```

### 4. Region Analysis (3 params)

```java
elongatedAspectRatio = 3.0      // Aspect ratio for elongated regions
mergeCloseFrac = 0.06           // Fraction for merging close regions
minClusterAreaPx = 400          // Minimum cluster area in pixels
```

### 5. Sidebar Detection (7 params)

```java
sidebarSearchFrac = 0.35        // Fraction of image to search for sidebar
sidebarMinWidthFrac = 0.03      // Minimum sidebar width fraction
sidebarMaxWidthFrac = 0.09      // Maximum sidebar width fraction
sidebarMinValidFrac = 0.6       // Minimum valid sidebar fraction
sidebarHueSpanDeg = 25          // Hue span in degrees for sidebar
sidebarMarginPx = 10            // Sidebar margin in pixels
textBottomBandFrac = 0.1        // Bottom band fraction for text
```

### 6. Overlay Masking (2 params)

```java
maskTopLeftOverlay = true           // Enable top-left overlay masking
topLeftBox = "0.0,0.0,0.5,0.12"     // Top-left box normalized coords (x1,y1,x2,y2)
```

### 7. Histogram Analysis (2 params)

```java
hBins = 256                     // Number of histogram bins
histDistanceMin = 0.03          // Minimum histogram distance
```

### 8. Red Background Detection (2 params)

```java
redBgRatioMinIncrease = 0.001   // Minimum red background ratio increase
redBgMinAbs = 0.001             // Minimum absolute red background
```

### 9. ROI Filters (2 params)

```java
roiSMin = 25                    // Minimum saturation for ROI
roiVMin = 50                    // Minimum value for ROI
```

### 10. Blue Background Detection (4 params)

```java
blueHLo = 90                    // Blue hue lower bound
blueHHi = 128                   // Blue hue upper bound
blueSMin = 40                   // Blue saturation minimum
blueVMin = 40                   // Blue value minimum
```

### 11. Black Background Detection (1 param)

```java
blackVHi = 40                   // Black value upper bound
```

### 12. White Background Detection (5 params)

```java
whiteBgSMax = 30                // White background saturation max
whiteBgVMin = 170               // White background value min
whiteBgExcludeNearWarmPx = 4    // Exclude pixels near warm areas
whiteBgColumnFrac = 0.25        // Column fraction for white background
whiteBgRowFrac = 0.15           // Row fraction for white background
```

---

## Total: 41 Parameters

### Data Types

- **Double (18 params)**: deltaKSigma, keepComponentMinRatio, faultRedRatio, potentialYellowRatio, fullwireHotFraction, elongatedAspectRatio, mergeCloseFrac, sidebarSearchFrac, sidebarMinWidthFrac, sidebarMaxWidthFrac, sidebarMinValidFrac, textBottomBandFrac, histDistanceMin, redBgRatioMinIncrease, redBgMinAbs, whiteBgColumnFrac, whiteBgRowFrac

- **Integer (21 params)**: deltaAbsMin, minBlobAreaPx, openIters, dilateIters, faultRedMinPixels, minClusterAreaPx, sidebarHueSpanDeg, sidebarMarginPx, hBins, roiSMin, roiVMin, blueHLo, blueHHi, blueSMin, blueVMin, blackVHi, whiteBgSMax, whiteBgVMin, whiteBgExcludeNearWarmPx

- **Boolean (1 param)**: maskTopLeftOverlay

- **String (1 param)**: topLeftBox (stored as "x1,y1,x2,y2")

---

## Usage Examples

### Creating New Configuration

```java
AnomalyDetectionConfig config = new AnomalyDetectionConfig();
config.setConfigName("thermal_analysis_v1");
config.setDescription("Thermal imaging configuration for transformer inspection");

// Set delta detection
config.setDeltaKSigma(0.28);
config.setDeltaAbsMin(5);

// Set fault detection
config.setFaultRedRatio(0.00007);
config.setElongatedAspectRatio(3.0);

// Set overlay masking
config.setMaskTopLeftOverlay(true);
config.setTopLeftBox("0,0,200,230");

// ... set remaining parameters

config.setIsActive(true);
configRepository.save(config);
```

### Converting to JSON for Flask API

```java
Map<String, Object> configMap = new HashMap<>();

// Add all parameters
configMap.put("delta_k_sigma", config.getDeltaKSigma());
configMap.put("fault_red_ratio", config.getFaultRedRatio());

// Convert topLeftBox string to List
String[] parts = config.getTopLeftBox().split(",");
List<Double> topLeftBox = new ArrayList<>();
for (String part : parts) {
    topLeftBox.add(Double.parseDouble(part.trim()));
}
configMap.put("top_left_box", topLeftBox);

String json = objectMapper.writeValueAsString(configMap);
```

### Parsing from Flask API Response

```java
JsonNode configNode = root.get("updated_config");

AnomalyDetectionConfig newConfig = new AnomalyDetectionConfig();
newConfig.setDeltaKSigma(configNode.get("delta_k_sigma").asDouble());
newConfig.setFaultRedRatio(configNode.get("fault_red_ratio").asDouble());

// Convert topLeftBox List to string
JsonNode topLeftBoxNode = configNode.get("top_left_box");
StringBuilder sb = new StringBuilder();
for (int i = 0; i < topLeftBoxNode.size(); i++) {
    if (i > 0) sb.append(",");
    sb.append(topLeftBoxNode.get(i).asDouble());
}
newConfig.setTopLeftBox(sb.toString());

configRepository.save(newConfig);
```

---

## SQL Query Examples

### Viewing All Parameters

```sql
SELECT
    id,
    config_name,
    delta_k_sigma,
    fault_red_ratio,
    elongated_aspect_ratio,
    top_left_box,
    is_active,
    created_at
FROM anomaly_detection_config
ORDER BY created_at DESC;
```

### Updating Specific Parameters

```sql
UPDATE anomaly_detection_config
SET
    delta_k_sigma = 0.30,
    fault_red_ratio = 0.0001,
    updated_at = NOW()
WHERE id = 1;
```

### Creating Test Configuration

```sql
INSERT INTO anomaly_detection_config (
    config_name, description, is_active,
    delta_k_sigma, delta_abs_min, min_blob_area_px,
    fault_red_ratio, elongated_aspect_ratio,
    mask_top_left_overlay, top_left_box,
    created_at, updated_at
) VALUES (
    'test_config', 'Test configuration', true,
    0.28, 5, 24,
    0.00007, 3.0,
    true, '0,0,200,230',
    NOW(), NOW()
);
```

---

## Common Issues & Solutions

### Issue 1: topLeftBox Format Error

**Problem**: "NumberFormatException when parsing top_left_box"
**Solution**: Ensure the string is exactly in format "x1,y1,x2,y2" with normalized coordinates (0.0-1.0) and no spaces

```java
// Correct - normalized coordinates (fractions of image dimensions)
config.setTopLeftBox("0.0,0.0,0.5,0.12");

// Incorrect
config.setTopLeftBox("0.0, 0.0, 0.5, 0.12");  // Has spaces
config.setTopLeftBox("0,0,200,230");           // Pixel coordinates instead of normalized
config.setTopLeftBox("0.0,0.0,0.5");           // Only 3 values
```

### Issue 2: NULL Values After Migration

**Problem**: "New columns are NULL for existing records"
**Solution**: Run an UPDATE query to set default values

```sql
UPDATE anomaly_detection_config
SET
    delta_k_sigma = 0.28,
    delta_abs_min = 5,
    min_blob_area_px = 24,
    -- ... all other defaults
WHERE delta_k_sigma IS NULL;
```

### Issue 3: JSON Serialization Mismatch

**Problem**: "Python expects array, Java sends string for top_left_box"
**Solution**: Always convert in createConfigJson()

```java
// In createConfigJson() method
String topLeftBoxStr = config.getTopLeftBox();
if (topLeftBoxStr != null && !topLeftBoxStr.isEmpty()) {
    String[] parts = topLeftBoxStr.split(",");
    List<Double> topLeftBox = new ArrayList<>();
    for (String part : parts) {
        topLeftBox.add(Double.parseDouble(part.trim()));
    }
    configMap.put("top_left_box", topLeftBox);  // Sends as array
}
```

---

## Testing Checklist

- [ ] Database migration completed successfully
- [ ] All 41 columns exist in database
- [ ] Default configuration can be created
- [ ] Configuration can be saved to database
- [ ] JSON serialization includes all 41 parameters
- [ ] topLeftBox converts correctly (String ↔ List)
- [ ] Flask API accepts the configuration format
- [ ] Training endpoint updates configuration
- [ ] New configuration is saved after training
- [ ] Old configuration is deactivated after training

---

## See Also

- [Configuration Update Summary](./CONFIGURATION_UPDATE_SUMMARY.md)
- [Classification Training README](./CLASSIFICATION_TRAINING_README.md)
