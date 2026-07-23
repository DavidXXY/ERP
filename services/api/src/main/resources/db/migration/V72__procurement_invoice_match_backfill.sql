UPDATE procurement_supplier_invoices
SET matched_amount = amount,
    approval_status = CASE
      WHEN match_status = 'MATCHED' THEN 'APPROVED'
      ELSE approval_status
    END,
    verification_status = CASE
      WHEN match_status = 'MATCHED' THEN 'VERIFIED'
      ELSE verification_status
    END
WHERE match_status = 'MATCHED'
  AND (matched_amount IS NULL OR matched_amount = 0);
