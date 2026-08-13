-- 供应商门户上传的开票资料，经采购审核后转为正式发票
CREATE TABLE IF NOT EXISTS supplier_invoice_submissions (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  supplier_id UUID NOT NULL REFERENCES procurement_suppliers(id),
  account_id UUID NOT NULL REFERENCES procurement_supplier_portal_accounts(id),
  order_id UUID NOT NULL REFERENCES procurement_purchase_orders(id),
  invoice_no VARCHAR(100) NOT NULL,
  amount NUMERIC(18,2) NOT NULL,
  tax_rate NUMERIC(10,4) NOT NULL DEFAULT 13,
  invoice_date DATE NOT NULL,
  remark VARCHAR(500),
  file_name VARCHAR(255) NOT NULL,
  object_key VARCHAR(500) NOT NULL,
  content_type VARCHAR(120),
  size_bytes BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  review_comment VARCHAR(500),
  reviewed_by VARCHAR(64),
  reviewed_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_invoice_submission_supplier
  ON supplier_invoice_submissions (tenant_id, supplier_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_invoice_submission_status
  ON supplier_invoice_submissions (tenant_id, status, created_at DESC);
