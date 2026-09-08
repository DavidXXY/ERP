-- 员工日报/周报/月报
CREATE TABLE emp_reports (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    report_type varchar(16) NOT NULL,
    report_date date NOT NULL,
    content text NOT NULL,
    progress_summary varchar(1000),
    plan_next varchar(2000),
    issue varchar(1000),
    employee_id uuid REFERENCES qual_employees(id),
    employee_name varchar(80) NOT NULL,
    department_name varchar(120),
    cc_user_ids text NOT NULL DEFAULT '[]',
    cc_names varchar(500),
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_emp_reports_type CHECK (report_type IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT ck_emp_reports_content CHECK (length(content) > 0)
);

CREATE INDEX idx_emp_reports_employee_type_date ON emp_reports (tenant_id, employee_id, report_type, report_date DESC);
CREATE INDEX idx_emp_reports_cc ON emp_reports (tenant_id, cc_user_ids);

-- 汇报权限
INSERT INTO sys_permissions (id, tenant_id, code, name, module, built_in, created_at, updated_at, version)
SELECT gen_random_uuid(), tenants.tenant_id, permission.code, permission.name, 'office', true, now(), now(), 0
FROM (SELECT DISTINCT tenant_id FROM sys_roles) tenants
CROSS JOIN (VALUES
    ('report:view', '汇报查看'),
    ('report:create', '汇报填写')
) AS permission(code, name)
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 默认给全部内置角色回报查看/填写能力（员工自助场景），管理员与负责人额外拥有下属收件箱
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE permission.code IN ('report:view', 'report:create')
  AND role.code IN ('ADMIN', 'EXECUTIVE_MANAGER', 'SALES_DIRECTOR', 'SALES_REP', 'FINANCE_MANAGER', 'FINANCE_ACCOUNTANT',
                    'FINANCE_CASHIER', 'HR_MANAGER', 'HR_SPECIALIST', 'PROCUREMENT_MANAGER', 'PROCUREMENT_SPECIALIST',
                    'PROJECT_MANAGER', 'PROJECT_MEMBER', 'OPS_MANAGER', 'OPS_ENGINEER', 'TECH_MANAGER', 'TECHNICIAN',
                    'QUALIFICATION_MANAGER', 'QUALIFICATION_SPECIALIST', 'SYSTEM_OPERATOR')
ON CONFLICT DO NOTHING;
