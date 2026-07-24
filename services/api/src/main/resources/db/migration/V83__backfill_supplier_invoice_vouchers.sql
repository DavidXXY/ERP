-- Recognize approved supplier invoices that predate supplier-invoice ledger posting.
INSERT INTO fin_accounting_vouchers (
    id, tenant_id, code, biz_type, biz_no, voucher_date, description, status,
    total_debit, total_credit, created_at, updated_at, version
)
SELECT
    md5('supplier-invoice-voucher:' || i.tenant_id || ':' || i.code)::uuid,
    i.tenant_id,
    'PZ-' || to_char(i.invoice_date, 'YYYYMMDD') || '-API-' || right(md5(i.tenant_id || ':' || i.code), 8),
    'SUPPLIER_INVOICE',
    i.code,
    i.invoice_date,
    '供应商发票 ' || i.invoice_no,
    'POSTED',
    i.amount,
    i.amount,
    now(),
    now(),
    0
FROM procurement_supplier_invoices i
WHERE i.approval_status = 'APPROVED'
  AND NOT EXISTS (
      SELECT 1
      FROM fin_accounting_vouchers v
      WHERE v.tenant_id = i.tenant_id
        AND v.biz_type = 'SUPPLIER_INVOICE'
        AND v.biz_no = i.code
  );

INSERT INTO fin_accounting_entries (
    id, tenant_id, voucher_id, account_code, account_name, debit, credit,
    summary, created_at, updated_at, version
)
SELECT
    md5('supplier-invoice-entry-debit:' || v.tenant_id || ':' || v.biz_no)::uuid,
    v.tenant_id,
    v.id,
    '1405',
    '库存商品',
    i.amount,
    0,
    i.invoice_no,
    now(),
    now(),
    0
FROM fin_accounting_vouchers v
JOIN procurement_supplier_invoices i
  ON i.tenant_id = v.tenant_id
 AND i.code = v.biz_no
WHERE v.biz_type = 'SUPPLIER_INVOICE'
  AND NOT EXISTS (
      SELECT 1 FROM fin_accounting_entries e WHERE e.voucher_id = v.id
  );

INSERT INTO fin_accounting_entries (
    id, tenant_id, voucher_id, account_code, account_name, debit, credit,
    summary, created_at, updated_at, version
)
SELECT
    md5('supplier-invoice-entry-credit:' || v.tenant_id || ':' || v.biz_no)::uuid,
    v.tenant_id,
    v.id,
    '2202',
    '应付账款',
    0,
    i.amount,
    i.code,
    now(),
    now(),
    0
FROM fin_accounting_vouchers v
JOIN procurement_supplier_invoices i
  ON i.tenant_id = v.tenant_id
 AND i.code = v.biz_no
WHERE v.biz_type = 'SUPPLIER_INVOICE'
  AND NOT EXISTS (
      SELECT 1
      FROM fin_accounting_entries e
      WHERE e.voucher_id = v.id
        AND e.account_code = '2202'
  );
