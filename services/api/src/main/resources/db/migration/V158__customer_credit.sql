-- 客户信用管控：信用额度与冻结标记，接单/发货时校验信用敞口

ALTER TABLE crm_customers ADD COLUMN IF NOT EXISTS credit_limit numeric(18,2);
ALTER TABLE crm_customers ADD COLUMN IF NOT EXISTS credit_blocked boolean NOT NULL DEFAULT false;

INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
VALUES
    ('00000000-0000-4000-8000-000000008401','default','crm:credit:manage','客户信用管理','crm',now(),now(),true,0)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code IN ('ADMIN')
  AND permission.code = 'crm:credit:manage'
ON CONFLICT DO NOTHING;