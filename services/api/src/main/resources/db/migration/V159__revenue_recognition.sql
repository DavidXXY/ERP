-- 收入确认引擎：合同履约里程碑，按履约进度确认收入（借 1122 应收账款 / 贷 6001 主营业务收入）

CREATE TABLE fin_contract_milestones (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  contract_id uuid NOT NULL,
  name varchar(160) NOT NULL,
  amount numeric(14,2) NOT NULL,
  planned_date date,
  status varchar(24) NOT NULL DEFAULT 'PENDING',
  recognized_date date,
  recognized_by varchar(80),
  remark varchar(500),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_fin_contract_milestones_contract ON fin_contract_milestones (tenant_id, contract_id);

CREATE TABLE fin_revenue_recognitions (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  contract_id uuid NOT NULL,
  milestone_id uuid,
  code varchar(64) NOT NULL,
  amount numeric(14,2) NOT NULL,
  recognize_date date NOT NULL,
  recognized_by varchar(80),
  remark varchar(500),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_fin_revenue_recognitions_code ON fin_revenue_recognitions (tenant_id, code);
CREATE INDEX idx_fin_revenue_recognitions_contract ON fin_revenue_recognitions (tenant_id, contract_id);

INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
VALUES
    ('00000000-0000-4000-8000-000000008501','default','finance:revenue:recognize','收入确认','finance',now(),now(),true,0)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code IN ('ADMIN')
  AND permission.code = 'finance:revenue:recognize'
ON CONFLICT DO NOTHING;