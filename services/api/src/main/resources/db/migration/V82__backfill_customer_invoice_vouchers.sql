-- Recognize revenue for invoices created before invoice posting was wired into the ledger.
INSERT INTO fin_accounting_vouchers (
    id, tenant_id, code, biz_type, biz_no, voucher_date, description, status,
    total_debit, total_credit, created_at, updated_at, version
)
SELECT
    md5('customer-invoice-voucher:' || r.tenant_id || ':' || r.code)::uuid,
    r.tenant_id,
    'PZ-' || to_char(r.invoice_date, 'YYYYMMDD') || '-INV-' || right(md5(r.tenant_id || ':' || r.code), 8),
    'INVOICE',
    r.code,
    r.invoice_date,
    '客户开票 ' || r.code,
    'POSTED',
    r.amount,
    r.amount,
    now(),
    now(),
    0
FROM fin_receivables r
WHERE r.invoice_no IS NOT NULL
  AND r.invoice_no <> ''
  AND r.invoice_date IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM fin_accounting_vouchers v
      WHERE v.tenant_id = r.tenant_id
        AND v.biz_type = 'INVOICE'
        AND v.biz_no = r.code
  );

INSERT INTO fin_accounting_entries (
    id, tenant_id, voucher_id, account_code, account_name, debit, credit,
    summary, created_at, updated_at, version
)
SELECT
    md5('customer-invoice-entry-debit:' || v.tenant_id || ':' || v.biz_no)::uuid,
    v.tenant_id,
    v.id,
    '1122',
    '应收账款',
    r.amount,
    0,
    r.invoice_no,
    now(),
    now(),
    0
FROM fin_accounting_vouchers v
JOIN fin_receivables r
  ON r.tenant_id = v.tenant_id
 AND r.code = v.biz_no
WHERE v.biz_type = 'INVOICE'
  AND NOT EXISTS (
      SELECT 1 FROM fin_accounting_entries e WHERE e.voucher_id = v.id
  );

INSERT INTO fin_accounting_entries (
    id, tenant_id, voucher_id, account_code, account_name, debit, credit,
    summary, created_at, updated_at, version
)
SELECT
    md5('customer-invoice-entry-credit:' || v.tenant_id || ':' || v.biz_no)::uuid,
    v.tenant_id,
    v.id,
    '6001',
    '主营业务收入',
    0,
    r.amount,
    r.code,
    now(),
    now(),
    0
FROM fin_accounting_vouchers v
JOIN fin_receivables r
  ON r.tenant_id = v.tenant_id
 AND r.code = v.biz_no
WHERE v.biz_type = 'INVOICE'
  AND NOT EXISTS (
      SELECT 1
      FROM fin_accounting_entries e
      WHERE e.voucher_id = v.id
        AND e.account_code = '6001'
  );
