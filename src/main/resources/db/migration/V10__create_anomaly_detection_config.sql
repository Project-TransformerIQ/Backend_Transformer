-- Create anomaly_detection_config table
CREATE TABLE anomaly_detection_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL UNIQUE,
    
    -- SSIM Configuration
    ssim_weight DOUBLE DEFAULT 0.5,
    ssim_threshold DOUBLE DEFAULT 0.85,
    
    -- MSE Configuration
    mse_weight DOUBLE DEFAULT 0.3,
    mse_threshold DOUBLE DEFAULT 1000.0,
    
    -- Histogram Configuration
    histogram_weight DOUBLE DEFAULT 0.2,
    histogram_threshold DOUBLE DEFAULT 0.7,
    
    -- Combined Threshold
    combined_threshold DOUBLE DEFAULT 0.75,
    
    -- Image Processing Configuration
    resize_width INT DEFAULT 800,
    resize_height INT DEFAULT 600,
    blur_kernel_size INT DEFAULT 5,
    
    -- Detection Configuration
    min_contour_area INT DEFAULT 100,
    dilation_iterations INT DEFAULT 2,
    erosion_iterations INT DEFAULT 1,
    
    -- Metadata
    is_active BOOLEAN DEFAULT FALSE,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes separately for H2 compatibility
CREATE INDEX idx_active ON anomaly_detection_config(is_active);
CREATE INDEX idx_config_name ON anomaly_detection_config(config_name);

-- Insert default configuration
INSERT INTO anomaly_detection_config (
    config_name,
    ssim_weight,
    ssim_threshold,
    mse_weight,
    mse_threshold,
    histogram_weight,
    histogram_threshold,
    combined_threshold,
    resize_width,
    resize_height,
    blur_kernel_size,
    min_contour_area,
    dilation_iterations,
    erosion_iterations,
    is_active,
    description
) VALUES (
    'default',
    0.5,
    0.85,
    0.3,
    1000.0,
    0.2,
    0.7,
    0.75,
    800,
    600,
    5,
    100,
    2,
    1,
    TRUE,
    'Default configuration for anomaly detection'
);
