CREATE TABLE IF NOT EXISTS supplier_portal_account_activities (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  account_id UUID NOT NULL REFERENCES procurement_supplier_portal_accounts(id) ON DELETE CASCADE,
  supplier_id UUID NOT NULL REFERENCES procurement_suppliers(id),
  action VARCHAR(40) NOT NULL,
  detail VARCHAR(500),
  ip VARCHAR(64),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_supplier_activity_account
  ON supplier_portal_account_activities (tenant_id, account_id, created_at DESC);
