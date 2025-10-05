ALTER TABLE transformer_images ADD COLUMN inspection_id BIGINT;
ALTER TABLE transformer_images ADD CONSTRAINT fk_images_inspection FOREIGN KEY (inspection_id) REFERENCES inspections(id) ON DELETE CASCADE;
