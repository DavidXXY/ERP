-- 分级审批规则：按金额区间路由审批级别与审批角色
CREATE TABLE IF NOT EXISTS procurement_approval_rules (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  rule_name VARCHAR(80) NOT NULL,
  min_amount NUMERIC(14,2),
  max_amount NUMERIC(14,2),
  approval_level VARCHAR(24) NOT NULL,
  required_role_code VARCHAR(64),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_proc_approval_rules
  ON procurement_approval_rules (tenant_id, enabled, sort_order);

INSERT INTO procurement_approval_rules (id, tenant_id, rule_name, min_amount, max_amount, approval_level, required_role_code, enabled, sort_order, created_at, updated_at, version)
SELECT '00000000-0000-4000-8000-000000000201', 'default', '小额采购-部门主管审批', 0, 10000, 'DEPARTMENT', 'PROCUREMENT_SPECIALIST', TRUE, 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM procurement_approval_rules WHERE id = '00000000-0000-4000-8000-000000000201');

INSERT INTO procurement_approval_rules (id, tenant_id, rule_name, min_amount, max_amount, approval_level, required_role_code, enabled, sort_order, created_at, updated_at, version)
SELECT '00000000-0000-4000-8000-000000000202', 'default', '中额采购-采购经理审批', 10000, 100000, 'MANAGER', 'PROCUREMENT_MANAGER', TRUE, 2, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM procurement_approval_rules WHERE id = '00000000-0000-4000-8000-000000000202');

INSERT INTO procurement_approval_rules (id, tenant_id, rule_name, min_amount, max_amount, approval_level, required_role_code, enabled, sort_order, created_at, updated_at, version)
SELECT '00000000-0000-4000-8000-000000000203', 'default', '大额采购-总经理审批', 100000, NULL, 'EXECUTIVE', 'EXECUTIVE_MANAGER', TRUE, 3, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM procurement_approval_rules WHERE id = '00000000-0000-4000-8000-000000000203');

-- 采购申请与审批记录补充审批级别
ALTER TABLE procurement_purchase_requests
  ADD COLUMN IF NOT EXISTS approval_level VARCHAR(24);
ALTER TABLE procurement_request_approval_records
  ADD COLUMN IF NOT EXISTS approval_level VARCHAR(24);
