-- 预收款与预付款：登记、核销到应收/应付，生成凭证

CREATE TABLE fin_advance_receipts (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  code varchar(64) NOT NULL,
  customer_id uuid NOT NULL,
  amount numeric(14,2) NOT NULL,
  settled_amount numeric(14,2) NOT NULL DEFAULT 0,
  received_date date NOT NULL,
  reference_no varchar(80),
  status varchar(16) NOT NULL DEFAULT 'OPEN',
  remark varchar(500),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_fin_advance_receipts_code ON fin_advance_receipts (tenant_id, code);
CREATE INDEX idx_fin_advance_receipts_customer ON fin_advance_receipts (tenant_id, customer_id);

CREATE TABLE fin_advance_payments (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  code varchar(64) NOT NULL,
  supplier_id uuid NOT NULL,
  amount numeric(14,2) NOT NULL,
  settled_amount numeric(14,2) NOT NULL DEFAULT 0,
  paid_date date NOT NULL,
  reference_no varchar(80),
  status varchar(16) NOT NULL DEFAULT 'OPEN',
  remark varchar(500),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_fin_advance_payments_code ON fin_advance_payments (tenant_id, code);
CREATE INDEX idx_fin_advance_payments_supplier ON fin_advance_payments (tenant_id, supplier_id);

INSERT INTO fin_accounting_accounts
    (tenant_id, code, name, category, normal_direction, cash_account, active, system_account)
VALUES
    ('default', '2203', '预收账款', 'LIABILITY', 'CREDIT', false, true, true),
    ('default', '1123', '预付账款', 'ASSET', 'DEBIT', false, true, true)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
VALUES
    ('00000000-0000-4000-8000-000000008301','default','finance:advance:manage','预收预付管理','finance',now(),now(),true,0)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code IN ('ADMIN')
  AND permission.code = 'finance:advance:manage'
ON CONFLICT DO NOTHING;