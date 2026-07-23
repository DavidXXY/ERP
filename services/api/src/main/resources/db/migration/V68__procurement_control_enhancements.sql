ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS approval_status VARCHAR(32) NOT NULL DEFAULT 'PENDING';
ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS approval_comment VARCHAR(500);
ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS approver_name VARCHAR(80);
ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE procurement_goods_receipts ADD COLUMN IF NOT EXISTS inspection_status VARCHAR(32) NOT NULL DEFAULT 'PASSED';
ALTER TABLE procurement_goods_receipts ADD COLUMN IF NOT EXISTS qualified_qty DECIMAL(14,2);
ALTER TABLE procurement_goods_receipts ADD COLUMN IF NOT EXISTS rejected_qty DECIMAL(14,2) NOT NULL DEFAULT 0;
ALTER TABLE procurement_goods_receipts ADD COLUMN IF NOT EXISTS inspector_name VARCHAR(80);
ALTER TABLE procurement_goods_receipts ADD COLUMN IF NOT EXISTS inspection_comment VARCHAR(500);
ALTER TABLE procurement_goods_receipts ADD COLUMN IF NOT EXISTS inspected_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_goods_receipts ADD COLUMN IF NOT EXISTS payable_due_date DATE;

CREATE TABLE IF NOT EXISTS procurement_inquiries (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, request_id UUID NOT NULL,
  code VARCHAR(64) NOT NULL, title VARCHAR(180) NOT NULL, deadline DATE,
  status VARCHAR(32) NOT NULL, created_by_name VARCHAR(80),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_procurement_inquiry_code UNIQUE (tenant_id, code)
);
CREATE TABLE IF NOT EXISTS procurement_supplier_quotes (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, inquiry_id UUID NOT NULL,
  supplier_id UUID NOT NULL, unit_price DECIMAL(14,2) NOT NULL, tax_rate DECIMAL(5,2) NOT NULL,
  delivery_date DATE, payment_terms VARCHAR(180), remark VARCHAR(500), selected BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS procurement_return_orders (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, code VARCHAR(64) NOT NULL,
  order_id UUID NOT NULL, receipt_id UUID NOT NULL, supplier_id UUID NOT NULL,
  quantity DECIMAL(14,2) NOT NULL, amount DECIMAL(14,2) NOT NULL, reason VARCHAR(500) NOT NULL,
  return_date DATE NOT NULL, handler_name VARCHAR(80) NOT NULL, status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_procurement_return_code UNIQUE (tenant_id, code)
);
CREATE TABLE IF NOT EXISTS procurement_supplier_invoices (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, code VARCHAR(64) NOT NULL,
  invoice_no VARCHAR(100) NOT NULL, order_id UUID NOT NULL, supplier_id UUID NOT NULL,
  amount DECIMAL(14,2) NOT NULL, tax_rate DECIMAL(5,2) NOT NULL, invoice_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL, match_status VARCHAR(32) NOT NULL, difference_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  remark VARCHAR(500),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_procurement_invoice_no UNIQUE (tenant_id, invoice_no)
);
CREATE INDEX IF NOT EXISTS idx_proc_inquiry_request ON procurement_inquiries(request_id);
CREATE INDEX IF NOT EXISTS idx_proc_quote_inquiry ON procurement_supplier_quotes(inquiry_id);
CREATE INDEX IF NOT EXISTS idx_proc_invoice_order ON procurement_supplier_invoices(order_id);
