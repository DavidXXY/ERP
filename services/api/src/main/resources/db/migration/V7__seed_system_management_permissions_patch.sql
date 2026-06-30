insert into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) values
('00000000-0000-4000-8000-000000002102', 'default', 'system:permission:create', '权限新增', 'system', now(), now()),
('00000000-0000-4000-8000-000000002103', 'default', 'system:permission:update', '权限编辑', 'system', now(), now()),
('00000000-0000-4000-8000-000000002104', 'default', 'system:permission:delete', '权限删除', 'system', now(), now()),
('00000000-0000-4000-8000-000000002201', 'default', 'system:organization:view', '组织架构查看', 'system', now(), now()),
('00000000-0000-4000-8000-000000002202', 'default', 'system:organization:create', '组织新增', 'system', now(), now()),
('00000000-0000-4000-8000-000000002203', 'default', 'system:organization:update', '组织编辑', 'system', now(), now()),
('00000000-0000-4000-8000-000000002204', 'default', 'system:organization:delete', '组织删除', 'system', now(), now())
on conflict (tenant_id, code) do update
set name = excluded.name,
    module = excluded.module,
    updated_at = excluded.updated_at;

insert into sys_role_permissions (role_id, permission_id)
select '00000000-0000-4000-8000-000000000101'::uuid, id
from sys_permissions
where tenant_id = 'default'
  and code in (
    'system:permission:create',
    'system:permission:update',
    'system:permission:delete',
    'system:organization:view',
    'system:organization:create',
    'system:organization:update',
    'system:organization:delete'
  )
on conflict do nothing;
