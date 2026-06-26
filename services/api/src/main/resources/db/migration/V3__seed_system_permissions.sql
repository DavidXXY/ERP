create table sys_role_permissions (
  role_id uuid not null references sys_roles(id) on delete cascade,
  permission_id uuid not null references sys_permissions(id) on delete cascade,
  primary key (role_id, permission_id)
);

insert into sys_organizations (
  id, tenant_id, code, name, org_type, enabled, created_at, updated_at
) values (
  '00000000-0000-4000-8000-000000000001', 'default', 'ROOT', '工程运维公司', 'COMPANY', true, now(), now()
);

insert into sys_roles (
  id, tenant_id, code, name, data_scope, created_at, updated_at
) values
('00000000-0000-4000-8000-000000000101', 'default', 'ADMIN', '系统管理员', 'ALL', now(), now()),
('00000000-0000-4000-8000-000000000102', 'default', 'CRM_MANAGER', '客户经理', 'DEPARTMENT', now(), now());

insert into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) values
('00000000-0000-4000-8000-000000001001', 'default', 'dashboard:view', '经营驾驶舱查看', 'dashboard', now(), now()),
('00000000-0000-4000-8000-000000001101', 'default', 'crm:customer:view', '客户池查看', 'crm', now(), now()),
('00000000-0000-4000-8000-000000001102', 'default', 'crm:customer:create', '客户新增', 'crm', now(), now()),
('00000000-0000-4000-8000-000000001201', 'default', 'crm:opportunity:view', '线索商机查看', 'crm', now(), now()),
('00000000-0000-4000-8000-000000001301', 'default', 'crm:contract:view', '维保合同查看', 'crm', now(), now()),
('00000000-0000-4000-8000-000000001401', 'default', 'procurement:view', '供应链采购查看', 'procurement', now(), now()),
('00000000-0000-4000-8000-000000001501', 'default', 'project:view', '项目管理查看', 'project', now(), now()),
('00000000-0000-4000-8000-000000001601', 'default', 'inventory:view', '备件仓储查看', 'inventory', now(), now()),
('00000000-0000-4000-8000-000000001701', 'default', 'finance:view', '财务资金查看', 'finance', now(), now()),
('00000000-0000-4000-8000-000000001801', 'default', 'system:view', '系统设置查看', 'system', now(), now());

insert into sys_users (
  id, tenant_id, org_id, username, display_name, password_hash, phone, email, enabled, created_at, updated_at
) values (
  '00000000-0000-4000-8000-000000000201',
  'default',
  '00000000-0000-4000-8000-000000000001',
  'admin',
  '系统管理员',
  '{noop}Admin@123',
  '13800000000',
  'admin@example.com',
  true,
  now(),
  now()
);

insert into sys_user_roles (user_id, role_id) values
('00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000101');

insert into sys_role_permissions (role_id, permission_id)
select '00000000-0000-4000-8000-000000000101'::uuid, id
from sys_permissions
where tenant_id = 'default';

insert into sys_role_permissions (role_id, permission_id)
select '00000000-0000-4000-8000-000000000102'::uuid, id
from sys_permissions
where code in (
  'dashboard:view',
  'crm:customer:view',
  'crm:customer:create',
  'crm:opportunity:view',
  'crm:contract:view'
);
