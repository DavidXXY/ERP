ALTER TABLE crm_quote_plans
  ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 13.00;

ALTER TABLE crm_quote_revisions
  ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 13.00;

ALTER TABLE crm_service_contracts
  ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 13.00;

ALTER TABLE procurement_purchase_requests
  ADD COLUMN IF NOT EXISTS unit_price NUMERIC(14,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 13.00,
  ADD COLUMN IF NOT EXISTS total_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00;

UPDATE procurement_purchase_requests
SET total_amount = quantity * unit_price
WHERE total_amount = 0 AND unit_price > 0;

ALTER TABLE procurement_purchase_orders
  ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 13.00;

ALTER TABLE procurement_goods_receipts
  ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 13.00;

ALTER TABLE fin_procurement_payables
  ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 13.00;
