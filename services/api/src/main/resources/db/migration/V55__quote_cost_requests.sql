CREATE TABLE IF NOT EXISTS crm_quote_cost_requests (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  quote_id UUID NOT NULL REFERENCES crm_quote_plans(id) ON DELETE CASCADE,
  opportunity_id UUID,
  customer_id UUID,
  status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
  requested_by VARCHAR(80) NOT NULL,
  requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  project_manager VARCHAR(80),
  labor_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
  material_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
  subcontract_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
  travel_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
  equipment_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
  risk_reserve NUMERIC(14, 2) NOT NULL DEFAULT 0,
  other_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
  suggested_price NUMERIC(14, 2),
  cost_remark VARCHAR(800),
  submitted_at TIMESTAMP WITH TIME ZONE,
  approved_by VARCHAR(80),
  approved_at TIMESTAMP WITH TIME ZONE,
  approval_comment VARCHAR(500),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  created_by VARCHAR(64),
  updated_by VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_quote_cost_requests_quote
  ON crm_quote_cost_requests(quote_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_quote_cost_requests_status
  ON crm_quote_cost_requests(status, requested_at DESC);

INSERT INTO sys_permissions (id, tenant_id, code, name, module, built_in, created_at, updated_at)
SELECT '00000000-0000-4000-8000-000000002506', 'default', 'crm:quote:cost', '报价成本询价', 'crm', true, now(), now()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permissions WHERE tenant_id = 'default' AND code = 'crm:quote:cost'
);

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.tenant_id = 'default'
  AND role.code = 'ADMIN'
  AND permission.code = 'crm:quote:cost'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permissions relation
    WHERE relation.role_id = role.id
      AND relation.permission_id = permission.id
  );
