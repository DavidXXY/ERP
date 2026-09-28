-- 固定资产模块：资产卡片、折旧计提、调拨、处置、盘点

-- 1) 资产卡片
CREATE TABLE IF NOT EXISTS fa_assets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    code varchar(64) NOT NULL,
    name varchar(160) NOT NULL,
    category varchar(64),
    acquisition_date date,
    original_value numeric(18,2) DEFAULT 0 NOT NULL,
    residual_value numeric(18,2) DEFAULT 0 NOT NULL,
    useful_life_months integer NOT NULL,
    depreciation_method varchar(32) DEFAULT 'STRAIGHT_LINE' NOT NULL,
    monthly_depreciation numeric(18,2) DEFAULT 0 NOT NULL,
    accumulated_depreciation numeric(18,2) DEFAULT 0 NOT NULL,
    last_depreciated_period varchar(7),
    status varchar(24) DEFAULT 'IN_USE' NOT NULL,
    location varchar(120),
    custodian varchar(80),
    custodian_user_id uuid,
    organization_id uuid,
    remark varchar(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT fa_assets_pkey PRIMARY KEY (id),
    CONSTRAINT uk_fa_assets_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ck_fa_assets_status CHECK (status IN ('IN_USE','IDLE','DISPOSED','SCRAPPED'))
);
CREATE INDEX IF NOT EXISTS idx_fa_assets_tenant_status ON fa_assets (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_fa_assets_tenant_code ON fa_assets (tenant_id, code);

-- 2) 折旧计提批次
CREATE TABLE IF NOT EXISTS fa_depreciation_runs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    code varchar(64) NOT NULL,
    period varchar(7) NOT NULL,
    run_date date NOT NULL,
    total_amount numeric(18,2) DEFAULT 0 NOT NULL,
    status varchar(16) DEFAULT 'POSTED' NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT fa_depreciation_runs_pkey PRIMARY KEY (id),
    CONSTRAINT uk_fa_depreciation_runs_tenant_period UNIQUE (tenant_id, period),
    CONSTRAINT ck_fa_depreciation_runs_status CHECK (status IN ('POSTED'))
);

-- 3) 折旧计提明细
CREATE TABLE IF NOT EXISTS fa_depreciation_lines (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    run_id uuid NOT NULL,
    asset_id uuid NOT NULL,
    period varchar(7) NOT NULL,
    amount numeric(18,2) DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT fa_depreciation_lines_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_fa_depreciation_lines_run ON fa_depreciation_lines (run_id);

-- 4) 资产调拨
CREATE TABLE IF NOT EXISTS fa_transfers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    code varchar(64) NOT NULL,
    asset_id uuid NOT NULL,
    from_location varchar(120),
    to_location varchar(120),
    from_custodian varchar(80),
    to_custodian varchar(80),
    transfer_date date NOT NULL,
    remark varchar(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT fa_transfers_pkey PRIMARY KEY (id),
    CONSTRAINT uk_fa_transfers_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX IF NOT EXISTS idx_fa_transfers_asset ON fa_transfers (asset_id);

-- 5) 资产处置/报废
CREATE TABLE IF NOT EXISTS fa_disposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    code varchar(64) NOT NULL,
    asset_id uuid NOT NULL,
    disposal_date date NOT NULL,
    method varchar(24),
    proceeds numeric(18,2) DEFAULT 0 NOT NULL,
    net_book_value numeric(18,2) DEFAULT 0 NOT NULL,
    gain_loss numeric(18,2) DEFAULT 0 NOT NULL,
    remark varchar(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT fa_disposals_pkey PRIMARY KEY (id),
    CONSTRAINT uk_fa_disposals_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX IF NOT EXISTS idx_fa_disposals_asset ON fa_disposals (asset_id);

-- 6) 资产盘点单
CREATE TABLE IF NOT EXISTS fa_counts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    code varchar(64) NOT NULL,
    count_date date NOT NULL,
    status varchar(16) DEFAULT 'DRAFT' NOT NULL,
    remark varchar(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT fa_counts_pkey PRIMARY KEY (id),
    CONSTRAINT uk_fa_counts_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ck_fa_counts_status CHECK (status IN ('DRAFT','POSTED'))
);

-- 7) 资产盘点明细
CREATE TABLE IF NOT EXISTS fa_count_lines (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    count_id uuid NOT NULL,
    asset_id uuid NOT NULL,
    book_quantity integer DEFAULT 1 NOT NULL,
    actual_quantity integer,
    difference integer,
    remark varchar(300),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT fa_count_lines_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_fa_count_lines_count ON fa_count_lines (count_id);

-- 8) 新增会计科目：1602 累计折旧、6711 资产处置损益
INSERT INTO fin_accounting_accounts
    (tenant_id, code, name, category, normal_direction, cash_account, active, system_account)
VALUES
    ('default', '1602', '累计折旧', 'ASSET', 'CREDIT', false, true, true),
    ('default', '1603', '固定资产减值准备', 'ASSET', 'CREDIT', false, true, true),
    ('default', '6711', '资产处置损益', 'EXPENSE', 'DEBIT', false, true, true)
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 9) 新增权限并授予 ADMIN 角色
INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
VALUES
    ('00000000-0000-4000-8000-000000006001', 'default', 'fixedasset:view', '固定资产查看', 'fixedasset', now(), now(), true, 0),
    ('00000000-0000-4000-8000-000000006002', 'default', 'fixedasset:manage', '固定资产管理', 'fixedasset', now(), now(), true, 0)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, perm.id
FROM sys_roles role
JOIN sys_permissions perm ON perm.code IN ('fixedasset:view', 'fixedasset:manage')
WHERE role.code = 'ADMIN'
  AND role.tenant_id = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permissions rp
    WHERE rp.role_id = role.id AND rp.permission_id = perm.id
  );
