# Anomaly Detection Configuration System

## Overview

This implementation adds a comprehensive configuration management system for anomaly detection. The configurations are stored in the database and sent to the Flask API during anomaly detection.

## What's Been Implemented

### 1. Database Schema (V10\_\_create_anomaly_detection_config.sql)

Created a new table `anomaly_detection_config` with the following configuration parameters:

#### SSIM Configuration

- `ssim_weight`: 0.5 (default)
- `ssim_threshold`: 0.85 (default)

#### MSE Configuration

- `mse_weight`: 0.3 (default)
- `mse_threshold`: 1000.0 (default)

#### Histogram Configuration

- `histogram_weight`: 0.2 (default)
- `histogram_threshold`: 0.7 (default)

#### Combined Threshold

- `combined_threshold`: 0.75 (default)

#### Image Processing Configuration

- `resize_width`: 800 (default)
- `resize_height`: 600 (default)
- `blur_kernel_size`: 5 (default)

#### Detection Configuration

- `min_contour_area`: 100 (default)
- `dilation_iterations`: 2 (default)
- `erosion_iterations`: 1 (default)

The migration script also inserts a default active configuration.

### 2. Model Class (AnomalyDetectionConfig.java)

- JPA entity representing the configuration
- Includes all configuration parameters
- Auto-timestamps (created_at, updated_at)
- Support for multiple configurations with one active at a time

### 3. Repository (AnomalyDetectionConfigRepository.java)

- Spring Data JPA repository
- Methods to find active configuration
- Methods to find configuration by name

### 4. Service Updates (AnomalyDetectionService.java)

Enhanced the service with:

- **Configuration retrieval**: Gets active config from database
- **JSON generation**: Converts config to JSON format
- **API integration**: Sends config file to Flask API along with images
- **Auto-creation**: Automatically creates default config if none exists

## How It Works

### Detection Flow:

1. When `detectAnomalies()` is called, it retrieves the active configuration from database
2. The configuration is converted to JSON format matching the expected structure
3. The JSON is sent as a file named "config.json" to the Flask API along with baseline and maintenance images
4. If no active config exists, a default one is created automatically

### Configuration JSON Format:

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

## Configuration Management

Configurations can be managed directly in the database through SQL or using a database management tool:

### View All Configurations:

```sql
SELECT * FROM anomaly_detection_config;
```

### Update Configuration Values:

```sql
UPDATE anomaly_detection_config
SET ssim_weight = 0.6,
    ssim_threshold = 0.90,
    combined_threshold = 0.80
WHERE config_name = 'default';
```

### Create New Configuration:

```sql
INSERT INTO anomaly_detection_config (
    config_name, ssim_weight, ssim_threshold, mse_weight, mse_threshold,
    histogram_weight, histogram_threshold, combined_threshold,
    resize_width, resize_height, blur_kernel_size,
    min_contour_area, dilation_iterations, erosion_iterations,
    is_active, description
) VALUES (
    'high_sensitivity', 0.6, 0.90, 0.3, 800.0, 0.1, 0.75, 0.80,
    1024, 768, 3, 50, 3, 1, FALSE,
    'High sensitivity configuration for detailed detection'
);
```

### Activate a Different Configuration:

```sql
-- First, deactivate all configurations
UPDATE anomaly_detection_config SET is_active = FALSE;

-- Then activate the desired one
UPDATE anomaly_detection_config SET is_active = TRUE WHERE config_name = 'high_sensitivity';
```

## Next Steps

To complete the integration:

1. **Update Flask API** to accept and use the configuration file:

   - Modify the `/detect-anomalies` endpoint to accept a `config` file
   - Parse the JSON configuration
   - Apply the configuration parameters during anomaly detection

2. **Run Database Migration**:

   ```bash
   # The migration will run automatically on next application startup
   # Or run manually through Flyway
   ```

3. **Test the Integration**:
   - Create different configuration profiles
   - Test anomaly detection with different configurations
   - Verify that configurations are properly stored and retrieved

## Benefits

- **Flexibility**: Easy to adjust detection parameters through database updates
- **Multiple Profiles**: Support different configurations for different scenarios
- **Database Persistence**: Configurations are stored and survive restarts
- **Version Control**: Track when configurations were created/modified
- **Automatic Fallback**: Creates default config if none exists
- **Simple Management**: Direct database access for configuration changes
