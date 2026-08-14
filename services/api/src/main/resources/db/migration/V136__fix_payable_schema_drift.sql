-- 修正 V135 与实体映射的 schema 偏差（ddl-auto=validate 启动失败）：
-- 1. cancelled_at 误建为 timestamptz，而 ProcurementPayable 实体映射为 date；
-- 2. procurement_supplier_invoice_payables 缺少 BaseEntity 的 created_by / updated_by / version。
ALTER TABLE public.fin_procurement_payables
  ALTER COLUMN cancelled_at TYPE date USING cancelled_at::date;

ALTER TABLE public.procurement_supplier_invoice_payables
  ADD COLUMN IF NOT EXISTS created_by varchar(64),
  ADD COLUMN IF NOT EXISTS updated_by varchar(64),
  ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
