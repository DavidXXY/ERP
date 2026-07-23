ALTER TABLE procurement_purchase_requests ADD COLUMN IF NOT EXISTS batch_id UUID;
ALTER TABLE procurement_purchase_requests ADD COLUMN IF NOT EXISTS batch_code VARCHAR(64);
ALTER TABLE procurement_purchase_requests ADD COLUMN IF NOT EXISTS batch_name VARCHAR(180);
ALTER TABLE procurement_purchase_requests ADD COLUMN IF NOT EXISTS line_no INTEGER;

UPDATE procurement_purchase_requests
SET batch_id = id,
    batch_code = code,
    batch_name = part_name,
    line_no = 1
WHERE batch_id IS NULL;

ALTER TABLE procurement_purchase_requests ALTER COLUMN batch_id SET NOT NULL;
ALTER TABLE procurement_purchase_requests ALTER COLUMN batch_code SET NOT NULL;
ALTER TABLE procurement_purchase_requests ALTER COLUMN line_no SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_proc_purchase_request_batch
  ON procurement_purchase_requests(tenant_id, batch_id, line_no);

