-- Add manual annotation fields to fault_regions table
ALTER TABLE fault_regions ADD COLUMN comment TEXT;
ALTER TABLE fault_regions ADD COLUMN is_manual BOOLEAN;
ALTER TABLE fault_regions ADD COLUMN created_at TIMESTAMP;
ALTER TABLE fault_regions ADD COLUMN created_by VARCHAR(255);

