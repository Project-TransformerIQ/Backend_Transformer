-- === Add columns (one by one), then set NOT NULL ===

-- transformer_no
ALTER TABLE transformers ADD COLUMN IF NOT EXISTS transformer_no VARCHAR(64);
ALTER TABLE transformers ALTER COLUMN transformer_no SET NOT NULL;

-- pole_no
ALTER TABLE transformers ADD COLUMN IF NOT EXISTS pole_no VARCHAR(64);
ALTER TABLE transformers ALTER COLUMN pole_no SET NOT NULL;

-- region
ALTER TABLE transformers ADD COLUMN IF NOT EXISTS region VARCHAR(64);
ALTER TABLE transformers ALTER COLUMN region SET NOT NULL;

-- transformer_type
ALTER TABLE transformers ADD COLUMN IF NOT EXISTS transformer_type VARCHAR(32);
ALTER TABLE transformers ALTER COLUMN transformer_type SET NOT NULL;

-- Add the allowed-values check constraint if it's not already there
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'transformers_type_chk'
  ) THEN
    ALTER TABLE transformers
      ADD CONSTRAINT transformers_type_chk
      CHECK (transformer_type IN ('BULK','DISTRIBUTION'));
  END IF;
END$$;

-- Drop old columns if present
ALTER TABLE transformers DROP COLUMN IF EXISTS name;
ALTER TABLE transformers DROP COLUMN IF EXISTS site;
ALTER TABLE transformers DROP COLUMN IF EXISTS rating_kva;
