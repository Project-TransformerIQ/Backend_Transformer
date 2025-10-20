-- Add lastModifiedAt and lastModifiedBy fields to fault_regions table for tracking updates
ALTER TABLE fault_regions ADD COLUMN last_modified_at TIMESTAMP;
ALTER TABLE fault_regions ADD COLUMN last_modified_by VARCHAR(255);
