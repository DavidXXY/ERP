-- 数据完整性加固：采购/合同核心表补外键、高频查询索引、金额精度修正

-- 1) 外键约束：先清孤儿（悬挂引用），再建约束

-- crm_contract_changes.contract_id -> crm_service_contracts(id)
DELETE FROM crm_contract_changes WHERE contract_id NOT IN (SELECT id FROM crm_service_contracts);
ALTER TABLE crm_contract_changes
  ADD CONSTRAINT fk_crm_contract_changes_contract
  FOREIGN KEY (contract_id) REFERENCES crm_service_contracts(id) ON DELETE RESTRICT;

-- procurement_supplier_quotes
DELETE FROM procurement_supplier_quotes WHERE inquiry_id NOT IN (SELECT id FROM procurement_inquiries);
DELETE FROM procurement_supplier_quotes WHERE supplier_id NOT IN (SELECT id FROM procurement_suppliers);
ALTER TABLE procurement_supplier_quotes
  ADD CONSTRAINT fk_supplier_quotes_inquiry FOREIGN KEY (inquiry_id) REFERENCES procurement_inquiries(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_supplier_quotes_supplier FOREIGN KEY (supplier_id) REFERENCES procurement_suppliers(id) ON DELETE RESTRICT;

-- procurement_supplier_quote_lines
DELETE FROM procurement_supplier_quote_lines WHERE quote_id NOT IN (SELECT id FROM procurement_supplier_quotes);
DELETE FROM procurement_supplier_quote_lines WHERE request_id NOT IN (SELECT id FROM procurement_purchase_requests);
ALTER TABLE procurement_supplier_quote_lines
  ADD CONSTRAINT fk_quote_lines_quote FOREIGN KEY (quote_id) REFERENCES procurement_supplier_quotes(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_quote_lines_request FOREIGN KEY (request_id) REFERENCES procurement_purchase_requests(id) ON DELETE RESTRICT;

-- procurement_inquiry_requests
DELETE FROM procurement_inquiry_requests WHERE inquiry_id NOT IN (SELECT id FROM procurement_inquiries);
DELETE FROM procurement_inquiry_requests WHERE request_id NOT IN (SELECT id FROM procurement_purchase_requests);
ALTER TABLE procurement_inquiry_requests
  ADD CONSTRAINT fk_inquiry_requests_inquiry FOREIGN KEY (inquiry_id) REFERENCES procurement_inquiries(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_inquiry_requests_request FOREIGN KEY (request_id) REFERENCES procurement_purchase_requests(id) ON DELETE RESTRICT;

-- procurement_collaboration_events
DELETE FROM procurement_collaboration_events WHERE supplier_id NOT IN (SELECT id FROM procurement_suppliers);
UPDATE procurement_collaboration_events SET order_id = NULL WHERE order_id NOT IN (SELECT id FROM procurement_purchase_orders);
ALTER TABLE procurement_collaboration_events
  ADD CONSTRAINT fk_collab_events_supplier FOREIGN KEY (supplier_id) REFERENCES procurement_suppliers(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_collab_events_order FOREIGN KEY (order_id) REFERENCES procurement_purchase_orders(id) ON DELETE SET NULL;

-- procurement_supplier_reviews
DELETE FROM procurement_supplier_reviews WHERE supplier_id NOT IN (SELECT id FROM procurement_suppliers);
ALTER TABLE procurement_supplier_reviews
  ADD CONSTRAINT fk_supplier_reviews_supplier FOREIGN KEY (supplier_id) REFERENCES procurement_suppliers(id) ON DELETE RESTRICT;

-- procurement_supplier_change_requests
DELETE FROM procurement_supplier_change_requests WHERE supplier_id NOT IN (SELECT id FROM procurement_suppliers);
ALTER TABLE procurement_supplier_change_requests
  ADD CONSTRAINT fk_supplier_change_supplier FOREIGN KEY (supplier_id) REFERENCES procurement_suppliers(id) ON DELETE RESTRICT;

-- procurement_supplier_invoices
DELETE FROM procurement_supplier_invoices WHERE order_id NOT IN (SELECT id FROM procurement_purchase_orders);
DELETE FROM procurement_supplier_invoices WHERE supplier_id NOT IN (SELECT id FROM procurement_suppliers);
UPDATE procurement_supplier_invoices SET payable_id = NULL WHERE payable_id NOT IN (SELECT id FROM fin_procurement_payables);
UPDATE procurement_supplier_invoices SET receipt_id = NULL WHERE receipt_id NOT IN (SELECT id FROM procurement_goods_receipts);
ALTER TABLE procurement_supplier_invoices
  ADD CONSTRAINT fk_supplier_invoice_order FOREIGN KEY (order_id) REFERENCES procurement_purchase_orders(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_supplier_invoice_supplier FOREIGN KEY (supplier_id) REFERENCES procurement_suppliers(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_supplier_invoice_payable FOREIGN KEY (payable_id) REFERENCES fin_procurement_payables(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_supplier_invoice_receipt FOREIGN KEY (receipt_id) REFERENCES procurement_goods_receipts(id) ON DELETE SET NULL;

-- procurement_return_orders
DELETE FROM procurement_return_orders WHERE order_id NOT IN (SELECT id FROM procurement_purchase_orders);
DELETE FROM procurement_return_orders WHERE receipt_id NOT IN (SELECT id FROM procurement_goods_receipts);
DELETE FROM procurement_return_orders WHERE supplier_id NOT IN (SELECT id FROM procurement_suppliers);
ALTER TABLE procurement_return_orders
  ADD CONSTRAINT fk_return_orders_order FOREIGN KEY (order_id) REFERENCES procurement_purchase_orders(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_return_orders_receipt FOREIGN KEY (receipt_id) REFERENCES procurement_goods_receipts(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_return_orders_supplier FOREIGN KEY (supplier_id) REFERENCES procurement_suppliers(id) ON DELETE RESTRICT;

-- 2) 高频过滤/排序列补索引
CREATE INDEX IF NOT EXISTS idx_fin_procurement_payables_order ON fin_procurement_payables(order_id);
CREATE INDEX IF NOT EXISTS idx_fin_procurement_payables_receipt ON fin_procurement_payables(receipt_id);
CREATE INDEX IF NOT EXISTS idx_fin_receivables_contract ON fin_receivables(contract_id);
CREATE INDEX IF NOT EXISTS idx_fin_accounting_entries_voucher ON fin_accounting_entries(voucher_id);
CREATE INDEX IF NOT EXISTS idx_procurement_orders_project ON procurement_purchase_orders(project_id);
CREATE INDEX IF NOT EXISTS idx_procurement_orders_part ON procurement_purchase_orders(part_id);
CREATE INDEX IF NOT EXISTS idx_procurement_orders_department ON procurement_purchase_orders(department_id);
CREATE INDEX IF NOT EXISTS idx_procurement_requests_project ON procurement_purchase_requests(project_id);
CREATE INDEX IF NOT EXISTS idx_procurement_requests_part ON procurement_purchase_requests(part_id);
CREATE INDEX IF NOT EXISTS idx_procurement_requests_department ON procurement_purchase_requests(department_id);
CREATE INDEX IF NOT EXISTS idx_sys_organizations_parent ON sys_organizations(parent_id);
CREATE INDEX IF NOT EXISTS idx_biz_project_timesheets_user ON biz_project_timesheets(user_id);
CREATE INDEX IF NOT EXISTS idx_biz_project_timesheets_project ON biz_project_timesheets(project_id);
CREATE INDEX IF NOT EXISTS idx_crm_contract_changes_contract ON crm_contract_changes(contract_id);

-- 3) 请假天数改精确 numeric（消除浮点漂移）
ALTER TABLE hr_leave_balances ALTER COLUMN total_days TYPE numeric(6,2) USING total_days::numeric(6,2);
ALTER TABLE hr_leave_balances ALTER COLUMN used_days TYPE numeric(6,2) USING used_days::numeric(6,2);
ALTER TABLE hr_leave_requests ALTER COLUMN total_days TYPE numeric(6,2) USING total_days::numeric(6,2);

-- 4) qual_performances.contract_amount varchar -> numeric
ALTER TABLE qual_performances
  ALTER COLUMN contract_amount TYPE numeric(14,2)
  USING NULLIF(trim(contract_amount), '')::numeric(14,2);

-- 5) 恢复合同金额 NOT NULL（V103 曾解除）
UPDATE crm_service_contracts SET amount = 0 WHERE amount IS NULL;
ALTER TABLE crm_service_contracts ALTER COLUMN amount SET NOT NULL;
