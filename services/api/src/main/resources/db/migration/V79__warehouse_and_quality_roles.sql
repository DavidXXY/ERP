INSERT INTO sys_permissions (
  id, tenant_id, code, name, module, built_in, created_at, updated_at, version
) VALUES (
  '00000000-0000-4000-8000-000000002499',
  'default',
  'procurement:receipt:inspect',
  '采购到货质检',
  'procurement',
  true,
  now(),
  now(),
  0
);

INSERT INTO sys_roles (
  id, tenant_id, code, name, data_scope, built_in, created_at, updated_at, version
) VALUES
  (
    '00000000-0000-4000-8000-000000000121',
    'default',
    'WAREHOUSE_OPERATOR',
    '仓储作业员',
    'DEPARTMENT',
    true,
    now(),
    now(),
    0
  ),
  (
    '00000000-0000-4000-8000-000000000122',
    'default',
    'QUALITY_INSPECTOR',
    '质量检验员',
    'DEPARTMENT',
    true,
    now(),
    now(),
    0
  );

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT '00000000-0000-4000-8000-000000000121', id
FROM sys_permissions
WHERE code IN (
  'procurement:view',
  'procurement:order:receive',
  'inventory:view',
  'inventory:movement:create',
  'inventory:issue:create',
  'inventory:return:create'
);

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT '00000000-0000-4000-8000-000000000122', id
FROM sys_permissions
WHERE code IN (
  'procurement:view',
  'procurement:receipt:inspect',
  'inventory:view'
);
