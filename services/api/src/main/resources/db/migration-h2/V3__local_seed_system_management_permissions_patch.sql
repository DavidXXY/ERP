merge into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) key (tenant_id, code) values
('00000000-0000-4000-8000-000000002102', 'default', 'system:permission:create', '权限新增', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002103', 'default', 'system:permission:update', '权限编辑', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002104', 'default', 'system:permission:delete', '权限删除', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002201', 'default', 'system:organization:view', '组织架构查看', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002202', 'default', 'system:organization:create', '组织新增', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002203', 'default', 'system:organization:update', '组织编辑', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002204', 'default', 'system:organization:delete', '组织删除', 'system', current_timestamp, current_timestamp);

insert into sys_role_permissions (role_id, permission_id)
select '00000000-0000-4000-8000-000000000101', p.id
from sys_permissions p
where p.tenant_id = 'default'
  and p.code in (
    'system:permission:create',
    'system:permission:update',
    'system:permission:delete',
    'system:organization:view',
    'system:organization:create',
    'system:organization:update',
    'system:organization:delete'
  )
  and not exists (
    select 1
    from sys_role_permissions rp
    where rp.role_id = '00000000-0000-4000-8000-000000000101'
      and rp.permission_id = p.id
  );
