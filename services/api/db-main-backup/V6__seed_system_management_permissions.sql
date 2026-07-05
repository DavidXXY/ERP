insert into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) values
('00000000-0000-4000-8000-000000001901', 'default', 'system:user:view', '员工管理查看', 'system', now(), now()),
('00000000-0000-4000-8000-000000001902', 'default', 'system:user:create', '员工新增', 'system', now(), now()),
('00000000-0000-4000-8000-000000001903', 'default', 'system:user:update', '员工编辑', 'system', now(), now()),
('00000000-0000-4000-8000-000000001904', 'default', 'system:user:delete', '员工删除', 'system', now(), now()),
('00000000-0000-4000-8000-000000001905', 'default', 'system:user:reset-password', '员工密码重置', 'system', now(), now()),
('00000000-0000-4000-8000-000000002001', 'default', 'system:role:view', '角色管理查看', 'system', now(), now()),
('00000000-0000-4000-8000-000000002002', 'default', 'system:role:create', '角色新增', 'system', now(), now()),
('00000000-0000-4000-8000-000000002003', 'default', 'system:role:update', '角色编辑', 'system', now(), now()),
('00000000-0000-4000-8000-000000002004', 'default', 'system:role:delete', '角色删除', 'system', now(), now()),
('00000000-0000-4000-8000-000000002101', 'default', 'system:permission:view', '权限管理查看', 'system', now(), now());

insert into sys_role_permissions (role_id, permission_id)
select '00000000-0000-4000-8000-000000000101'::uuid, id
from sys_permissions
where tenant_id = 'default' and code like 'system:%'
on conflict (role_id, permission_id) do nothing;
