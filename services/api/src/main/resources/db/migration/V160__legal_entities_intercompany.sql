-- 多账套多法人：法人/账套主数据、内部交易台账、会计凭证实体维度

CREATE TABLE sys_legal_entities (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  code varchar(64) NOT NULL,
  name varchar(160) NOT NULL,
  entity_type varchar(24) NOT NULL DEFAULT 'LEGAL_ENTITY',
  currency varchar(8) NOT NULL DEFAULT 'CNY',
  active boolean NOT NULL DEFAULT true,
  remark varchar(500),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_legal_entities_code ON sys_legal_entities (tenant_id, code);

CREATE TABLE fin_intercompany_transactions (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  code varchar(64) NOT NULL,
  from_entity_id uuid NOT NULL,
  to_entity_id uuid NOT NULL,
  amount numeric(14,2) NOT NULL,
  direction varchar(24) NOT NULL DEFAULT 'RECEIVABLE',
  transaction_date date NOT NULL,
  reason varchar(500),
  status varchar(16) NOT NULL DEFAULT 'POSTED',
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_fin_intercompany_transactions_code ON fin_intercompany_transactions (tenant_id, code);
CREATE INDEX idx_fin_intercompany_transactions_from ON fin_intercompany_transactions (tenant_id, from_entity_id);
CREATE INDEX idx_fin_intercompany_transactions_to ON fin_intercompany_transactions (tenant_id, to_entity_id);

-- 会计凭证/分录增加法人（账套）维度，默认空表示主账套
ALTER TABLE fin_accounting_vouchers ADD COLUMN IF NOT EXISTS entity_id uuid;
ALTER TABLE fin_accounting_entries ADD COLUMN IF NOT EXISTS entity_id uuid;

INSERT INTO fin_accounting_accounts
    (tenant_id, code, name, category, normal_direction, cash_account, active, system_account)
VALUES
    ('default', '1221', '内部往来', 'ASSET', 'DEBIT', false, true, true),
    ('default', '2204', '内部往来-应付', 'LIABILITY', 'CREDIT', false, true, true)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
VALUES
    ('00000000-0000-4000-8000-000000008601','default','ledger:entity:manage','账套法人管理','system',now(),now(),true,0)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code IN ('ADMIN')
  AND permission.code = 'ledger:entity:manage'
ON CONFLICT DO NOTHING;