ALTER TABLE procurement_supplier_portal_accounts
  ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS mfa_secret VARCHAR(1024),
  ADD COLUMN IF NOT EXISTS mfa_recovery_codes TEXT;
