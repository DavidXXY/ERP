-- 薪酬/社保/个税模块：薪资项、工资核算批次、核算明细、核算参数

-- 1) 薪资项
CREATE TABLE IF NOT EXISTS payroll_salary_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    employee_id uuid NOT NULL,
    name varchar(80) NOT NULL,
    amount numeric(18,2) DEFAULT 0 NOT NULL,
    item_type varchar(24) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    remark varchar(300),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT payroll_salary_items_pkey PRIMARY KEY (id),
    CONSTRAINT ck_payroll_salary_items_type CHECK (item_type IN ('BASE','ALLOWANCE','BONUS','DEDUCTION','SOCIAL','TAX'))
);
CREATE INDEX IF NOT EXISTS idx_payroll_salary_items_employee ON payroll_salary_items (tenant_id, employee_id);

-- 2) 工资核算批次
CREATE TABLE IF NOT EXISTS payroll_runs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    code varchar(80) NOT NULL,
    period varchar(7) NOT NULL,
    run_date date NOT NULL,
    total_gross numeric(18,2) DEFAULT 0 NOT NULL,
    total_social numeric(18,2) DEFAULT 0 NOT NULL,
    total_tax numeric(18,2) DEFAULT 0 NOT NULL,
    total_net numeric(18,2) DEFAULT 0 NOT NULL,
    status varchar(16) DEFAULT 'DRAFT' NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT payroll_runs_pkey PRIMARY KEY (id),
    CONSTRAINT uk_payroll_runs_tenant_period UNIQUE (tenant_id, period),
    CONSTRAINT ck_payroll_runs_status CHECK (status IN ('DRAFT','CONFIRMED','PAID'))
);
CREATE INDEX IF NOT EXISTS idx_payroll_runs_tenant_period ON payroll_runs (tenant_id, period);

-- 3) 工资核算明细
CREATE TABLE IF NOT EXISTS payroll_run_lines (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    run_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    employee_name varchar(120),
    base_salary numeric(18,2) DEFAULT 0 NOT NULL,
    allowances numeric(18,2) DEFAULT 0 NOT NULL,
    bonus numeric(18,2) DEFAULT 0 NOT NULL,
    deductions numeric(18,2) DEFAULT 0 NOT NULL,
    social_insurance numeric(18,2) DEFAULT 0 NOT NULL,
    tax numeric(18,2) DEFAULT 0 NOT NULL,
    net_pay numeric(18,2) DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT payroll_run_lines_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_payroll_run_lines_run ON payroll_run_lines (run_id);
CREATE INDEX IF NOT EXISTS idx_payroll_run_lines_employee ON payroll_run_lines (tenant_id, employee_id);

-- 4) 核算参数
CREATE TABLE IF NOT EXISTS payroll_configs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    config_key varchar(64) NOT NULL,
    config_value numeric(18,4) DEFAULT 0 NOT NULL,
    remark varchar(300),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT payroll_configs_pkey PRIMARY KEY (id),
    CONSTRAINT uk_payroll_configs_tenant_key UNIQUE (tenant_id, config_key)
);

INSERT INTO payroll_configs (tenant_id, config_key, config_value, remark)
VALUES
    ('default', 'social_rate', 0.1050, '个人社保公积金合计比例（简化）'),
    ('default', 'tax_threshold', 5000.0000, '个税起征点（月）')
ON CONFLICT (tenant_id, config_key) DO NOTHING;

-- 5) 新增会计科目
INSERT INTO fin_accounting_accounts
    (tenant_id, code, name, category, normal_direction, cash_account, active, system_account)
VALUES
    ('default', '2212', '其他应付款-社保公积金', 'LIABILITY', 'CREDIT', false, true, true),
    ('default', '22210102', '应交个人所得税', 'LIABILITY', 'CREDIT', false, true, true)
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 6) 新增权限并授予 ADMIN 角色
INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
VALUES
    ('00000000-0000-4000-8000-000000007001', 'default', 'payroll:view', '薪酬管理查看', 'payroll', now(), now(), true, 0),
    ('00000000-0000-4000-8000-000000007002', 'default', 'payroll:manage', '薪酬核算与发放', 'payroll', now(), now(), true, 0)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, perm.id
FROM sys_roles role
JOIN sys_permissions perm ON perm.code IN ('payroll:view', 'payroll:manage')
WHERE role.code = 'ADMIN'
  AND role.tenant_id = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permissions rp
    WHERE rp.role_id = role.id AND rp.permission_id = perm.id
  );
