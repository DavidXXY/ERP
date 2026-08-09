CREATE TABLE IF NOT EXISTS supplier_portal_notifications (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  account_id UUID NOT NULL,
  supplier_id UUID NOT NULL,
  type VARCHAR(40) NOT NULL,
  title VARCHAR(180) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  related_type VARCHAR(80),
  related_id UUID,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  read_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_supplier_notif_account
  ON supplier_portal_notifications (tenant_id, account_id, created_at DESC);
