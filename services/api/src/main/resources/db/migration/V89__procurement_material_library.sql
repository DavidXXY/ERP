INSERT INTO sys_permissions (
    id, tenant_id, code, name, module, built_in, created_at, updated_at, version
)
SELECT gen_random_uuid(), 'default', 'procurement:material:manage',
       '物料库维护', 'procurement', true, now(), now(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permissions
    WHERE tenant_id = 'default' AND code = 'procurement:material:manage'
);

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission
  ON permission.tenant_id = role.tenant_id
 AND permission.code = 'procurement:material:manage'
WHERE role.tenant_id = 'default'
  AND role.code IN ('ADMIN', 'PROCUREMENT_MANAGER', 'PROCUREMENT_SPECIALIST')
ON CONFLICT DO NOTHING;
