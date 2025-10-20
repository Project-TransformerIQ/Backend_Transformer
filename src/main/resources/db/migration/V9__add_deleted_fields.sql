-- Add isDeleted and deletedAt fields to fault_regions table for soft deletion
ALTER TABLE fault_regions ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE fault_regions ADD COLUMN deleted_at TIMESTAMP;
