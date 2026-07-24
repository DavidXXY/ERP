-- Local migrations historically skipped the production V68 procurement controls migration.
-- Create/upgrade those objects here so both an existing local database and an empty test database
-- reach the same schema before applying the end-to-end governance fields.
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
  delivery_date DATE, payment_terms VARCHAR(180), remark VARCHAR(500),
  selected BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS procurement_return_orders (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, code VARCHAR(64) NOT NULL,
  order_id UUID NOT NULL, receipt_id UUID NOT NULL, supplier_id UUID NOT NULL,
  quantity DECIMAL(14,2) NOT NULL, amount DECIMAL(14,2) NOT NULL,
  reason VARCHAR(500) NOT NULL, return_date DATE NOT NULL,
  handler_name VARCHAR(80) NOT NULL, status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_procurement_return_code UNIQUE (tenant_id, code)
);
CREATE TABLE IF NOT EXISTS procurement_supplier_invoices (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, code VARCHAR(64) NOT NULL,
  invoice_no VARCHAR(100) NOT NULL, order_id UUID NOT NULL, supplier_id UUID NOT NULL,
  amount DECIMAL(14,2) NOT NULL, tax_rate DECIMAL(5,2) NOT NULL, invoice_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL, match_status VARCHAR(32) NOT NULL,
  difference_amount DECIMAL(14,2) NOT NULL DEFAULT 0, remark VARCHAR(500),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_procurement_invoice_no UNIQUE (tenant_id, invoice_no)
);
CREATE INDEX IF NOT EXISTS idx_proc_inquiry_request ON procurement_inquiries(request_id);
CREATE INDEX IF NOT EXISTS idx_proc_quote_inquiry ON procurement_supplier_quotes(inquiry_id);
CREATE INDEX IF NOT EXISTS idx_proc_invoice_order ON procurement_supplier_invoices(order_id);

ALTER TABLE procurement_inquiries ADD COLUMN IF NOT EXISTS sourcing_method VARCHAR(32) NOT NULL DEFAULT 'COMPETITIVE';
ALTER TABLE procurement_inquiries ADD COLUMN IF NOT EXISTS min_quote_count INTEGER NOT NULL DEFAULT 3;
ALTER TABLE procurement_inquiries ADD COLUMN IF NOT EXISTS exception_reason VARCHAR(500);
ALTER TABLE procurement_inquiries ADD COLUMN IF NOT EXISTS selected_quote_id UUID;
ALTER TABLE procurement_inquiries ADD COLUMN IF NOT EXISTS selection_reason VARCHAR(1000);
ALTER TABLE procurement_inquiries ADD COLUMN IF NOT EXISTS selected_by_name VARCHAR(80);
ALTER TABLE procurement_inquiries ADD COLUMN IF NOT EXISTS selected_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE procurement_supplier_quotes ADD COLUMN IF NOT EXISTS currency VARCHAR(8) NOT NULL DEFAULT 'CNY';
ALTER TABLE procurement_supplier_quotes ADD COLUMN IF NOT EXISTS freight_amount NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE procurement_supplier_quotes ADD COLUMN IF NOT EXISTS other_cost_amount NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE procurement_supplier_quotes ADD COLUMN IF NOT EXISTS technical_score NUMERIC(5,2) NOT NULL DEFAULT 100;
ALTER TABLE procurement_supplier_quotes ADD COLUMN IF NOT EXISTS commercial_score NUMERIC(5,2) NOT NULL DEFAULT 100;
ALTER TABLE procurement_supplier_quotes ADD COLUMN IF NOT EXISTS total_score NUMERIC(5,2) NOT NULL DEFAULT 100;
ALTER TABLE procurement_supplier_quotes ADD COLUMN IF NOT EXISTS valid_until DATE;

ALTER TABLE procurement_purchase_requests ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE procurement_purchase_requests ADD COLUMN IF NOT EXISTS source_reference VARCHAR(120);

ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS inquiry_id UUID;
ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS contract_id UUID;
ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS currency VARCHAR(8) NOT NULL DEFAULT 'CNY';
ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS freight_amount NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS source_reason VARCHAR(1000);
ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_purchase_orders ADD COLUMN IF NOT EXISTS order_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE procurement_goods_receipts ADD COLUMN IF NOT EXISTS client_request_id VARCHAR(80);
ALTER TABLE procurement_goods_receipts ADD COLUMN IF NOT EXISTS asn_no VARCHAR(80);

ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS payable_id UUID;
ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS receipt_id UUID;
ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS matched_amount NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS approval_status VARCHAR(32) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS verification_status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED';
ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS client_request_id VARCHAR(80);
ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS approved_by_name VARCHAR(80);
ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_supplier_invoices ADD COLUMN IF NOT EXISTS attachment_document_id UUID;

ALTER TABLE procurement_return_orders ADD COLUMN IF NOT EXISTS replacement_qty NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE procurement_return_orders ADD COLUMN IF NOT EXISTS credit_amount NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE procurement_return_orders ADD COLUMN IF NOT EXISTS claim_amount NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE procurement_return_orders ADD COLUMN IF NOT EXISTS corrective_action VARCHAR(1000);
ALTER TABLE procurement_return_orders ADD COLUMN IF NOT EXISTS supplier_response VARCHAR(1000);
ALTER TABLE procurement_return_orders ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS procurement_contracts (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, contract_no VARCHAR(80) NOT NULL,
  name VARCHAR(180) NOT NULL, supplier_id UUID NOT NULL, amount NUMERIC(14,2) NOT NULL,
  currency VARCHAR(8) NOT NULL DEFAULT 'CNY', start_date DATE, end_date DATE,
  payment_terms VARCHAR(500), status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  approval_status VARCHAR(32) NOT NULL DEFAULT 'PENDING', version_no INTEGER NOT NULL DEFAULT 1,
  parent_contract_id UUID, change_reason VARCHAR(1000), submitted_by_name VARCHAR(80),
  submitted_at TIMESTAMP WITH TIME ZONE, approved_by_name VARCHAR(80),
  approval_comment VARCHAR(500), approved_at TIMESTAMP WITH TIME ZONE, remark VARCHAR(1000),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_procurement_contract_no UNIQUE (tenant_id, contract_no, version_no)
);

CREATE TABLE IF NOT EXISTS procurement_supplier_change_requests (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, supplier_id UUID NOT NULL,
  change_type VARCHAR(40) NOT NULL, proposed_admission_status VARCHAR(40),
  proposed_risk_status VARCHAR(32), proposed_bank_name VARCHAR(120),
  proposed_bank_account VARCHAR(512), proposed_settlement_terms VARCHAR(160),
  reason VARCHAR(1000) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  requested_by_name VARCHAR(80) NOT NULL, reviewed_by_name VARCHAR(80),
  review_comment VARCHAR(500), reviewed_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS procurement_supplier_reviews (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, supplier_id UUID NOT NULL,
  review_period VARCHAR(20) NOT NULL, on_time_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
  quality_rate NUMERIC(5,2) NOT NULL DEFAULT 0, invoice_match_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
  response_score NUMERIC(5,2) NOT NULL DEFAULT 100, total_score NUMERIC(5,2) NOT NULL DEFAULT 0,
  grade VARCHAR(20) NOT NULL, reviewer_name VARCHAR(80) NOT NULL,
  improvement_action VARCHAR(1000), status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_supplier_review_period UNIQUE (tenant_id, supplier_id, review_period)
);

CREATE TABLE IF NOT EXISTS procurement_collaboration_events (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, supplier_id UUID NOT NULL,
  order_id UUID, event_type VARCHAR(40) NOT NULL, reference_no VARCHAR(100),
  event_date DATE NOT NULL, promised_date DATE, quantity NUMERIC(14,2),
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN', content VARCHAR(1000),
  created_by_name VARCHAR(80) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64), updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS procurement_action_logs (
  id UUID PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, source_type VARCHAR(40) NOT NULL,
  source_id UUID NOT NULL, action_type VARCHAR(40) NOT NULL, operator_name VARCHAR(80) NOT NULL,
  details VARCHAR(2000), created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(64),
  updated_by VARCHAR(64), version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_proc_receipt_client_request ON procurement_goods_receipts(tenant_id, client_request_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_proc_invoice_client_request ON procurement_supplier_invoices(tenant_id, client_request_id);
CREATE INDEX IF NOT EXISTS idx_proc_contract_supplier ON procurement_contracts(tenant_id, supplier_id, status);
CREATE INDEX IF NOT EXISTS idx_proc_supplier_change ON procurement_supplier_change_requests(tenant_id, supplier_id, status);
CREATE INDEX IF NOT EXISTS idx_proc_supplier_review ON procurement_supplier_reviews(tenant_id, supplier_id, review_period);
CREATE INDEX IF NOT EXISTS idx_proc_collab_order ON procurement_collaboration_events(tenant_id, order_id, event_type);
CREATE INDEX IF NOT EXISTS idx_proc_action_source ON procurement_action_logs(tenant_id, source_type, source_id, created_at);
