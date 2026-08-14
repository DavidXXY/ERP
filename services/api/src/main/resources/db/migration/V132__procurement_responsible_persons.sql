-- 采购板块负责人/经办人补齐
-- 采购发票经办人：登记/审核发票时记录操作人
ALTER TABLE procurement_supplier_invoices
  ADD COLUMN IF NOT EXISTS handler_name VARCHAR(80);

-- 采购应付经办人：到货入库生成应付时记录经办人
ALTER TABLE fin_procurement_payables
  ADD COLUMN IF NOT EXISTS handler_name VARCHAR(80);

-- 供应商负责采购：内部采购负责人
ALTER TABLE procurement_suppliers
  ADD COLUMN IF NOT EXISTS purchaser_name VARCHAR(80);
