ALTER TABLE crm_quote_plans ADD COLUMN IF NOT EXISTS archived boolean NOT NULL DEFAULT false;
