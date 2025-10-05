-- Create fault_regions table
CREATE TABLE fault_regions (
    db_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    region_id INTEGER,
    type VARCHAR(100),
    dominant_color VARCHAR(50),
    bbox_x INTEGER,
    bbox_y INTEGER,
    bbox_width INTEGER,
    bbox_height INTEGER,
    bbox_area_px INTEGER,
    centroid_x INTEGER,
    centroid_y INTEGER,
    aspect_ratio DOUBLE,
    elongated BOOLEAN,
    connected_to_wire BOOLEAN,
    tag VARCHAR(50),
    confidence DOUBLE,
    image_id BIGINT,
    FOREIGN KEY (image_id) REFERENCES transformer_images(id) ON DELETE CASCADE
);

-- Create fault_region_colors table for storing RGB values
CREATE TABLE fault_region_colors (
    fault_region_id BIGINT,
    color_value INTEGER,
    FOREIGN KEY (fault_region_id) REFERENCES fault_regions(db_id) ON DELETE CASCADE
);

-- Create display_metadata table
CREATE TABLE display_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME,
    image_id BIGINT,
    FOREIGN KEY (image_id) REFERENCES transformer_images(id) ON DELETE CASCADE
);

-- Create display_box_colors table for storing box color mappings
CREATE TABLE display_box_colors (
    display_metadata_id BIGINT,
    color_key VARCHAR(50),
    color_value VARCHAR(50),
    FOREIGN KEY (display_metadata_id) REFERENCES display_metadata(id) ON DELETE CASCADE
);
