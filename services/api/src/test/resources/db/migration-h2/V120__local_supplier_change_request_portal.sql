-- 供应商变更申请支持门户自助提交：企业名称/信用代码变更与来源标记
ALTER TABLE procurement_supplier_change_requests ADD COLUMN IF NOT EXISTS proposed_name VARCHAR(160);
ALTER TABLE procurement_supplier_change_requests ADD COLUMN IF NOT EXISTS proposed_credit_code VARCHAR(80);
ALTER TABLE procurement_supplier_change_requests ADD COLUMN IF NOT EXISTS request_source VARCHAR(16) NOT NULL DEFAULT 'INTERNAL';

CREATE INDEX IF NOT EXISTS idx_proc_supplier_change_supplier
  ON procurement_supplier_change_requests (tenant_id, supplier_id, created_at);
