INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code = 'HR_MANAGER'
  AND permission.code IN (
    'system:user:view',
    'system:user:create',
    'system:user:update',
    'system:user:reset-password'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
