CREATE TABLE IF NOT EXISTS procurement_order_documents (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  order_id UUID NOT NULL REFERENCES procurement_purchase_orders(id) ON DELETE CASCADE,
  file_name VARCHAR(240) NOT NULL,
  object_key VARCHAR(500) NOT NULL,
  content_type VARCHAR(160),
  size_bytes BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  uploaded_by VARCHAR(80),
  uploaded_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_proc_order_doc_order
  ON procurement_order_documents (tenant_id, order_id, created_at DESC);
