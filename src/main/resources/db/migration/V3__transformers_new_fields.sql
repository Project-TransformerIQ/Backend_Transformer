-- Add columns if missing, then enforce NOT NULL safely (works on H2)

-- transformer_no
ALTER TABLE transformers ADD COLUMN IF NOT EXISTS transformer_no VARCHAR(64);
UPDATE transformers SET transformer_no = 'UNKNOWN' WHERE transformer_no IS NULL;
ALTER TABLE transformers ALTER COLUMN transformer_no SET NOT NULL;

-- pole_no
ALTER TABLE transformers ADD COLUMN IF NOT EXISTS pole_no VARCHAR(64);
UPDATE transformers SET pole_no = 'UNKNOWN' WHERE pole_no IS NULL;
ALTER TABLE transformers ALTER COLUMN pole_no SET NOT NULL;

-- region
ALTER TABLE transformers ADD COLUMN IF NOT EXISTS region VARCHAR(64);
UPDATE transformers SET region = 'UNKNOWN' WHERE region IS NULL;
ALTER TABLE transformers ALTER COLUMN region SET NOT NULL;

-- transformer_type
ALTER TABLE transformers ADD COLUMN IF NOT EXISTS transformer_type VARCHAR(32);
UPDATE transformers SET transformer_type = 'BULK' WHERE transformer_type IS NULL;
ALTER TABLE transformers ALTER COLUMN transformer_type SET NOT NULL;

-- Drop old columns if present
ALTER TABLE transformers DROP COLUMN IF EXISTS name;
ALTER TABLE transformers DROP COLUMN IF EXISTS site;
ALTER TABLE transformers DROP COLUMN IF EXISTS rating_kva;
