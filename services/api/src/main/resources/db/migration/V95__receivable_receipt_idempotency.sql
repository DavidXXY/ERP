CREATE UNIQUE INDEX IF NOT EXISTS uk_receipt_tenant_reference
  ON fin_receivable_receipts (tenant_id, reference_no);
