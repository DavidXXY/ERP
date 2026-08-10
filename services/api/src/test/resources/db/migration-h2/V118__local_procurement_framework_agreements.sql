CREATE TABLE IF NOT EXISTS procurement_framework_agreements (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  code VARCHAR(64) NOT NULL,
  title VARCHAR(180) NOT NULL,
  supplier_id UUID NOT NULL REFERENCES procurement_suppliers(id),
  valid_from DATE NOT NULL,
  valid_to DATE NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(1000),
  created_by_name VARCHAR(80),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_proc_framework_supplier
  ON procurement_framework_agreements (tenant_id, supplier_id, status);

CREATE TABLE IF NOT EXISTS procurement_framework_agreement_items (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  agreement_id UUID NOT NULL REFERENCES procurement_framework_agreements(id) ON DELETE CASCADE,
  part_id UUID NOT NULL,
  part_name VARCHAR(160) NOT NULL,
  unit_price NUMERIC(14,2) NOT NULL,
  tax_rate NUMERIC(5,2) NOT NULL DEFAULT 13,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_proc_framework_items_agreement
  ON procurement_framework_agreement_items (tenant_id, agreement_id);
