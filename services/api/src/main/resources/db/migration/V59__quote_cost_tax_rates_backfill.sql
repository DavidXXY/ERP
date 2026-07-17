ALTER TABLE crm_quote_cost_requests
  ADD COLUMN IF NOT EXISTS labor_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 6,
  ADD COLUMN IF NOT EXISTS material_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 13,
  ADD COLUMN IF NOT EXISTS subcontract_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 6,
  ADD COLUMN IF NOT EXISTS equipment_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 13;

UPDATE crm_quote_cost_requests
SET labor_tax_rate = COALESCE(labor_tax_rate, 6),
    material_tax_rate = COALESCE(material_tax_rate, 13),
    subcontract_tax_rate = COALESCE(subcontract_tax_rate, 6),
    equipment_tax_rate = COALESCE(equipment_tax_rate, 13);
