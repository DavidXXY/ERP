merge into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) key (tenant_id, code) values
('00000000-0000-4000-8000-000000002314', 'default', 'crm:customer:update', '客户档案编辑', 'crm', current_timestamp, current_timestamp);

insert into sys_role_permissions (role_id, permission_id)
select r.id, p.id
from sys_roles r
join sys_permissions p on p.tenant_id = r.tenant_id
where r.tenant_id = 'default'
  and r.code in ('ADMIN', 'CRM_MANAGER')
  and p.code = 'crm:customer:update'
  and not exists (
    select 1 from sys_role_permissions rp
    where rp.role_id = r.id and rp.permission_id = p.id
  );
