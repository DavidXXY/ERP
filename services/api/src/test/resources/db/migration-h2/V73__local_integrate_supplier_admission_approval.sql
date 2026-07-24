ALTER TABLE procurement_suppliers ADD COLUMN IF NOT EXISTS admission_submitted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_suppliers ADD COLUMN IF NOT EXISTS admission_reviewed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE procurement_suppliers ADD COLUMN IF NOT EXISTS admission_reviewer_name VARCHAR(80);
ALTER TABLE procurement_suppliers ADD COLUMN IF NOT EXISTS admission_review_comment VARCHAR(500);

UPDATE procurement_suppliers
SET admission_status = 'PENDING'
WHERE admission_status IS NULL OR TRIM(admission_status) = '';

UPDATE procurement_suppliers
SET admission_submitted_at = created_at
WHERE admission_status = 'PENDING' AND admission_submitted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_proc_supplier_admission
  ON procurement_suppliers(tenant_id, admission_status, created_at);
