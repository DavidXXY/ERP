-- 采购应付增强（H2 测试库镜像）
ALTER TABLE fin_procurement_payables ADD COLUMN IF NOT EXISTS adjusted_amount numeric(14, 2) DEFAULT 0 NOT NULL;
ALTER TABLE fin_procurement_payables ADD COLUMN IF NOT EXISTS cancel_reason varchar(500);
ALTER TABLE fin_procurement_payables ADD COLUMN IF NOT EXISTS cancelled_by varchar(80);
ALTER TABLE fin_procurement_payables ADD COLUMN IF NOT EXISTS cancelled_at timestamp with time zone;

ALTER TABLE fin_payment_records ADD COLUMN IF NOT EXISTS source_type varchar(20) DEFAULT 'APPLICATION' NOT NULL;
ALTER TABLE fin_payment_records ADD COLUMN IF NOT EXISTS note varchar(500);
ALTER TABLE fin_payment_records ALTER COLUMN application_id DROP NOT NULL;

CREATE TABLE IF NOT EXISTS fin_payment_application_payables (
  id uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  application_id uuid NOT NULL REFERENCES fin_payment_applications(id),
  payable_id uuid NOT NULL REFERENCES fin_procurement_payables(id),
  allocated_amount numeric(14, 2) NOT NULL,
  created_at timestamp NOT NULL DEFAULT current_timestamp,
  updated_at timestamp NOT NULL DEFAULT current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_payment_app_payable
  ON fin_payment_application_payables (payable_id, application_id);
INSERT INTO fin_payment_application_payables
  (id, tenant_id, application_id, payable_id, allocated_amount, created_at, updated_at, version)
SELECT RANDOM_UUID(), tenant_id, id, payable_id, requested_amount, created_at, updated_at, 0
FROM fin_payment_applications;

CREATE TABLE IF NOT EXISTS fin_procurement_payable_adjustments (
  id uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  code varchar(64) NOT NULL,
  payable_id uuid NOT NULL REFERENCES fin_procurement_payables(id),
  order_id uuid NOT NULL REFERENCES procurement_purchase_orders(id),
  supplier_id uuid NOT NULL REFERENCES procurement_suppliers(id),
  adjustment_type varchar(32) NOT NULL,
  amount numeric(14, 2) NOT NULL,
  reason varchar(500),
  operator_name varchar(80) NOT NULL,
  applied_at date NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'APPLIED',
  source varchar(32) NOT NULL DEFAULT 'MANUAL',
  source_id uuid,
  created_at timestamp NOT NULL DEFAULT current_timestamp,
  updated_at timestamp NOT NULL DEFAULT current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uk_payable_adjustment_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX IF NOT EXISTS idx_payable_adjustment_payable
  ON fin_procurement_payable_adjustments (payable_id, applied_at);

ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS verified_by varchar(80);
ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS verified_at timestamp with time zone;
ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS verification_comment varchar(500);

CREATE TABLE IF NOT EXISTS procurement_supplier_invoice_payables (
  id uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  invoice_id uuid NOT NULL REFERENCES procurement_supplier_invoices(id),
  payable_id uuid NOT NULL REFERENCES fin_procurement_payables(id),
  created_at timestamp NOT NULL DEFAULT current_timestamp,
  updated_at timestamp NOT NULL DEFAULT current_timestamp
);
CREATE INDEX IF NOT EXISTS idx_supplier_invoice_payable
  ON procurement_supplier_invoice_payables (invoice_id, payable_id);
INSERT INTO procurement_supplier_invoice_payables
  (id, tenant_id, invoice_id, payable_id, created_at, updated_at)
SELECT RANDOM_UUID(), tenant_id, id, payable_id, created_at, updated_at
FROM procurement_supplier_invoices
WHERE payable_id IS NOT NULL;

ALTER TABLE procurement_suppliers
  ADD COLUMN IF NOT EXISTS payment_terms_days integer NOT NULL DEFAULT 30;

INSERT INTO fin_accounting_accounts
    (tenant_id, code, name, category, normal_direction, cash_account, active, system_account)
VALUES
    ('default', '6111', '其他业务收入', 'REVENUE', 'CREDIT', false, true, true);
