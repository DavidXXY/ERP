-- Add delete permission codes for CRM entities
-- These were previously missing: delete operations were guarded by manual ADMIN role check

insert into sys_permissions (id, tenant_id, code, name, module, created_at, updated_at)
values
  (gen_random_uuid(), 'default', 'crm:customer:delete', '客户删除', 'crm', now(), now()),
  (gen_random_uuid(), 'default', 'crm:opportunity:delete', '商机删除', 'crm', now(), now()),
  (gen_random_uuid(), 'default', 'crm:quote:delete', '报价方案删除', 'crm', now(), now()),
  (gen_random_uuid(), 'default', 'crm:contract:delete', '合同删除', 'crm', now(), now()),
  (gen_random_uuid(), 'default', 'crm:followup:delete', '跟进记录删除', 'crm', now(), now())
on conflict (tenant_id, code) do nothing;

-- Assign to ADMIN role
insert into sys_role_permissions (role_id, permission_id)
select r.id, p.id
from sys_roles r
cross join sys_permissions p
where r.tenant_id = 'default'
  and r.code = 'ADMIN'
  and p.code in (
    'crm:customer:delete',
    'crm:opportunity:delete',
    'crm:quote:delete',
    'crm:contract:delete',
    'crm:followup:delete'
  )
  and not exists (
    select 1 from sys_role_permissions rp
    where rp.role_id = r.id and rp.permission_id = p.id
  );
