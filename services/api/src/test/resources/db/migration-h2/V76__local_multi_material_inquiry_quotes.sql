CREATE TABLE IF NOT EXISTS procurement_supplier_quote_lines (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  quote_id UUID NOT NULL,
  request_id UUID NOT NULL,
  quantity NUMERIC(14,2) NOT NULL,
  unit_price NUMERIC(14,2) NOT NULL,
  tax_rate NUMERIC(5,2) NOT NULL,
  delivery_date DATE,
  remark VARCHAR(500),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_proc_quote_request_line UNIQUE (tenant_id, quote_id, request_id)
);

CREATE INDEX IF NOT EXISTS idx_proc_quote_lines_quote
  ON procurement_supplier_quote_lines(tenant_id, quote_id);
CREATE INDEX IF NOT EXISTS idx_proc_quote_lines_request
  ON procurement_supplier_quote_lines(tenant_id, request_id);
