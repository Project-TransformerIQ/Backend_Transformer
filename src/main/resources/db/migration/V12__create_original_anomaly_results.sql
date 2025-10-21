CREATE TABLE IF NOT EXISTS original_anomaly_results (
    id BIGSERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL UNIQUE,
    anomaly_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (image_id) REFERENCES transformer_images(id) ON DELETE CASCADE
);

CREATE INDEX idx_original_anomaly_results_image_id ON original_anomaly_results(image_id);

