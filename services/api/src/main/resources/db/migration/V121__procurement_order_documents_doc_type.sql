ALTER TABLE procurement_order_documents
  ADD COLUMN doc_type VARCHAR(20) NOT NULL DEFAULT 'OTHER';

CREATE INDEX IF NOT EXISTS idx_proc_order_doc_type
  ON procurement_order_documents (tenant_id, order_id, doc_type);
