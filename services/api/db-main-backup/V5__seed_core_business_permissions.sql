insert into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) values
('00000000-0000-4000-8000-000000001402', 'default', 'procurement:supplier:create', '供应商新增', 'procurement', now(), now()),
('00000000-0000-4000-8000-000000001403', 'default', 'procurement:purchase:create', '采购单据新增', 'procurement', now(), now()),
('00000000-0000-4000-8000-000000001502', 'default', 'project:create', '项目新增', 'project', now(), now()),
('00000000-0000-4000-8000-000000001602', 'default', 'inventory:part:create', '备件新增', 'inventory', now(), now()),
('00000000-0000-4000-8000-000000001603', 'default', 'inventory:movement:create', '库存流水新增', 'inventory', now(), now());

insert into sys_role_permissions (role_id, permission_id)
select '00000000-0000-4000-8000-000000000101'::uuid, id
from sys_permissions
where tenant_id = 'default'
  and code in (
    'procurement:supplier:create',
    'procurement:purchase:create',
    'project:create',
    'inventory:part:create',
    'inventory:movement:create'
  );
