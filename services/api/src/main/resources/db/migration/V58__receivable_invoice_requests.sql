ALTER TABLE fin_receivables
  ADD COLUMN IF NOT EXISTS invoice_requested boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS invoice_requested_by varchar(80),
  ADD COLUMN IF NOT EXISTS invoice_requested_at timestamp with time zone,
  ADD COLUMN IF NOT EXISTS invoice_request_remark varchar(500);

UPDATE fin_receivables
SET invoice_requested = false
WHERE invoice_requested IS NULL;

UPDATE fin_receivables
SET invoice_requested = true,
    invoice_requested_by = COALESCE(invoice_requested_by, '历史已开票'),
    invoice_requested_at = COALESCE(invoice_requested_at, now())
WHERE invoice_no IS NOT NULL
  AND invoice_no <> ''
  AND invoice_requested = false;

ALTER TABLE fin_receivables
  ALTER COLUMN invoice_requested SET DEFAULT false,
  ALTER COLUMN invoice_requested SET NOT NULL;
