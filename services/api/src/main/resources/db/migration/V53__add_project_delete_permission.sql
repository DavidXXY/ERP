INSERT INTO sys_permissions (id, tenant_id, code, name, module, built_in, created_at, updated_at)
SELECT '00000000-0000-4000-8000-000000002504', 'default', 'project:delete', '项目删除', 'project', true, now(), now()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permissions WHERE tenant_id = 'default' AND code = 'project:delete'
);

UPDATE sys_permissions
SET name = '项目删除',
    module = 'project',
    built_in = true,
    updated_at = now()
WHERE tenant_id = 'default'
  AND code = 'project:delete';

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.tenant_id = 'default'
  AND role.code = 'ADMIN'
  AND permission.code = 'project:delete'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permissions relation
    WHERE relation.role_id = role.id
      AND relation.permission_id = permission.id
  );
