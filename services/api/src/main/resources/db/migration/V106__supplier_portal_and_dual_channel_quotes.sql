CREATE TABLE IF NOT EXISTS procurement_supplier_portal_accounts (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  supplier_id UUID NOT NULL REFERENCES procurement_suppliers(id),
  email VARCHAR(160) NOT NULL,
  phone VARCHAR(40),
  contact_name VARCHAR(80) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
  review_comment VARCHAR(500),
  reviewed_by_name VARCHAR(80),
  reviewed_at TIMESTAMP WITH TIME ZONE,
  last_login_at TIMESTAMP WITH TIME ZONE,
  auth_version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_portal_account_email
  ON procurement_supplier_portal_accounts (tenant_id, email);
CREATE INDEX IF NOT EXISTS idx_supplier_portal_account_supplier
  ON procurement_supplier_portal_accounts (tenant_id, supplier_id, status);

CREATE TABLE IF NOT EXISTS procurement_supplier_portal_documents (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  supplier_id UUID NOT NULL REFERENCES procurement_suppliers(id),
  account_id UUID NOT NULL REFERENCES procurement_supplier_portal_accounts(id),
  document_type VARCHAR(40) NOT NULL,
  document_name VARCHAR(240) NOT NULL,
  object_key VARCHAR(500) NOT NULL,
  content_type VARCHAR(160),
  size_bytes BIGINT NOT NULL,
  valid_to DATE,
  review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  review_comment VARCHAR(500),
  reviewed_by_name VARCHAR(80),
  reviewed_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_supplier_portal_document_supplier
  ON procurement_supplier_portal_documents (tenant_id, supplier_id, created_at DESC);

CREATE TABLE IF NOT EXISTS procurement_inquiry_invitations (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  inquiry_id UUID NOT NULL REFERENCES procurement_inquiries(id),
  supplier_id UUID NOT NULL REFERENCES procurement_suppliers(id),
  status VARCHAR(32) NOT NULL DEFAULT 'INVITED',
  invited_by_name VARCHAR(80),
  invited_at TIMESTAMP WITH TIME ZONE NOT NULL,
  viewed_at TIMESTAMP WITH TIME ZONE,
  responded_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_procurement_inquiry_invitation
  ON procurement_inquiry_invitations (tenant_id, inquiry_id, supplier_id);
CREATE INDEX IF NOT EXISTS idx_procurement_invitation_supplier
  ON procurement_inquiry_invitations (tenant_id, supplier_id, status, invited_at DESC);

ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS submission_source VARCHAR(32) NOT NULL DEFAULT 'INTERNAL_ENTRY';
ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS submission_status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED';
ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS version_no INTEGER NOT NULL DEFAULT 1;
ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS submitted_by_type VARCHAR(32) NOT NULL DEFAULT 'INTERNAL_USER';
ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS submitted_by_id UUID;
ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS submitted_by_name VARCHAR(80);
ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS confirmed_by_account_id UUID;
ALTER TABLE procurement_supplier_quotes
  ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP WITH TIME ZONE;

UPDATE procurement_supplier_quotes
SET submitted_at = created_at
WHERE submitted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_procurement_quote_supplier_status
  ON procurement_supplier_quotes (tenant_id, inquiry_id, supplier_id, submission_status);

CREATE TABLE IF NOT EXISTS procurement_supplier_quote_revisions (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  quote_id UUID NOT NULL REFERENCES procurement_supplier_quotes(id),
  version_no INTEGER NOT NULL,
  submission_source VARCHAR(32) NOT NULL,
  submitted_by_type VARCHAR(32) NOT NULL,
  submitted_by_id UUID,
  submitted_by_name VARCHAR(80),
  submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
  snapshot_json TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_procurement_quote_revision
  ON procurement_supplier_quote_revisions (tenant_id, quote_id, version_no);
