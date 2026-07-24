CREATE TABLE IF NOT EXISTS procurement_inquiry_requests (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  inquiry_id UUID NOT NULL,
  request_id UUID NOT NULL,
  requested_qty NUMERIC(14,2) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_proc_inquiry_request_link UNIQUE (tenant_id, inquiry_id, request_id)
);

INSERT INTO procurement_inquiry_requests (
  id, tenant_id, inquiry_id, request_id, requested_qty,
  created_at, updated_at, created_by, updated_by, version
)
SELECT inquiry.id, inquiry.tenant_id, inquiry.id, inquiry.request_id,
       request.quantity, inquiry.created_at, inquiry.updated_at,
       inquiry.created_by, inquiry.updated_by, 0
FROM procurement_inquiries inquiry
JOIN procurement_purchase_requests request ON request.id = inquiry.request_id
WHERE NOT EXISTS (
  SELECT 1
  FROM procurement_inquiry_requests link
  WHERE link.tenant_id = inquiry.tenant_id
    AND link.inquiry_id = inquiry.id
    AND link.request_id = inquiry.request_id
);

CREATE INDEX IF NOT EXISTS idx_proc_inquiry_requests_inquiry
  ON procurement_inquiry_requests(tenant_id, inquiry_id);
CREATE INDEX IF NOT EXISTS idx_proc_inquiry_requests_request
  ON procurement_inquiry_requests(tenant_id, request_id);

