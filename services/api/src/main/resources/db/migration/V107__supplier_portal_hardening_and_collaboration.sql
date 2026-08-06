ALTER TABLE procurement_supplier_portal_accounts
  ADD COLUMN IF NOT EXISTS profile_draft_json TEXT;
ALTER TABLE procurement_supplier_portal_accounts
  ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE procurement_supplier_portal_accounts
  ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE procurement_inquiry_invitations
  ADD COLUMN IF NOT EXISTS registration_code_hash VARCHAR(255);
ALTER TABLE procurement_inquiry_invitations
  ADD COLUMN IF NOT EXISTS registration_code_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_inquiry_invitations
  ADD COLUMN IF NOT EXISTS registration_code_used_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_inquiry_invitations
  ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING';
ALTER TABLE procurement_inquiry_invitations
  ADD COLUMN IF NOT EXISTS delivery_attempt_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE procurement_inquiry_invitations
  ADD COLUMN IF NOT EXISTS last_delivery_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_inquiry_invitations
  ADD COLUMN IF NOT EXISTS delivery_error VARCHAR(500);
ALTER TABLE procurement_inquiry_invitations
  ADD COLUMN IF NOT EXISTS declined_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_inquiry_invitations
  ADD COLUMN IF NOT EXISTS decline_reason VARCHAR(500);

ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS declined_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS decline_reason VARCHAR(500);

CREATE TABLE IF NOT EXISTS procurement_supplier_quote_attachments (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  quote_id UUID NOT NULL REFERENCES procurement_supplier_quotes(id) ON DELETE CASCADE,
  supplier_id UUID NOT NULL REFERENCES procurement_suppliers(id),
  account_id UUID NOT NULL REFERENCES procurement_supplier_portal_accounts(id),
  attachment_type VARCHAR(40) NOT NULL,
  file_name VARCHAR(240) NOT NULL,
  object_key VARCHAR(500) NOT NULL,
  content_type VARCHAR(160),
  size_bytes BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_supplier_quote_attachment
  ON procurement_supplier_quote_attachments (tenant_id, quote_id, created_at DESC);

CREATE TABLE IF NOT EXISTS procurement_inquiry_clarifications (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  inquiry_id UUID NOT NULL REFERENCES procurement_inquiries(id) ON DELETE CASCADE,
  supplier_id UUID NOT NULL REFERENCES procurement_suppliers(id),
  account_id UUID NOT NULL REFERENCES procurement_supplier_portal_accounts(id),
  question VARCHAR(1000) NOT NULL,
  asked_at TIMESTAMP WITH TIME ZONE NOT NULL,
  answer VARCHAR(2000),
  answered_by_name VARCHAR(80),
  answered_at TIMESTAMP WITH TIME ZONE,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_inquiry_clarification_supplier
  ON procurement_inquiry_clarifications (tenant_id, inquiry_id, supplier_id, asked_at DESC);

INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
SELECT gen_random_uuid(), tenant.tenant_id, permission.code, permission.name, 'procurement', now(), now(), true, 0
FROM (SELECT DISTINCT tenant_id FROM sys_permissions) tenant
CROSS JOIN (VALUES
  ('procurement:portal-account:approve', '供应商门户账号审核'),
  ('procurement:portal-document:approve', '供应商门户资质审核'),
  ('procurement:supplier:admission', '供应商准入审核')
) AS permission(code, name)
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permissions existing
  WHERE existing.tenant_id = tenant.tenant_id AND existing.code = permission.code
);

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE permission.code IN (
  'procurement:portal-account:approve',
  'procurement:portal-document:approve',
  'procurement:supplier:admission'
)
AND role.code IN ('ADMIN', 'PROCUREMENT_MANAGER')
AND NOT EXISTS (
  SELECT 1 FROM sys_role_permissions existing
  WHERE existing.role_id = role.id AND existing.permission_id = permission.id
);
