INSERT INTO sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at, built_in, version
)
SELECT gen_random_uuid(), tenant.tenant_id, 'system:health:view', '系统运行状态查看', 'system', now(), now(), true, 0
FROM (SELECT DISTINCT tenant_id FROM sys_roles) tenant
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permissions permission
  WHERE permission.tenant_id = tenant.tenant_id AND permission.code = 'system:health:view'
);

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code IN ('ADMIN', 'SYSTEM_OPERATOR')
  AND permission.code = 'system:health:view'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permissions existing
    WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );
