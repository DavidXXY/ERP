ALTER TABLE crm_quote_cost_requests
  ADD COLUMN IF NOT EXISTS labor_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 6,
  ADD COLUMN IF NOT EXISTS material_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 13,
  ADD COLUMN IF NOT EXISTS subcontract_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 13,
  ADD COLUMN IF NOT EXISTS travel_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS equipment_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 13,
  ADD COLUMN IF NOT EXISTS risk_reserve_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS other_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 13;

ALTER TABLE crm_quote_cost_requests
  ALTER COLUMN labor_tax_rate SET DEFAULT 6,
  ALTER COLUMN travel_tax_rate SET DEFAULT 0;

UPDATE crm_quote_cost_requests
SET labor_tax_rate = 6
WHERE labor_tax_rate = 13;

UPDATE crm_quote_cost_requests
SET travel_tax_rate = 0;
