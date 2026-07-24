-- Add delete permission codes for CRM entities (H2 local)
-- These were previously missing: delete operations were guarded by manual ADMIN role check

merge into sys_permissions (id, tenant_id, code, name, module, created_at, updated_at) key (tenant_id, code) values
  (RANDOM_UUID(), 'default', 'crm:customer:delete', '客户删除', 'crm', current_timestamp, current_timestamp),
  (RANDOM_UUID(), 'default', 'crm:opportunity:delete', '商机删除', 'crm', current_timestamp, current_timestamp),
  (RANDOM_UUID(), 'default', 'crm:quote:delete', '报价方案删除', 'crm', current_timestamp, current_timestamp),
  (RANDOM_UUID(), 'default', 'crm:contract:delete', '合同删除', 'crm', current_timestamp, current_timestamp),
  (RANDOM_UUID(), 'default', 'crm:followup:delete', '跟进记录删除', 'crm', current_timestamp, current_timestamp);

-- Assign to ADMIN role
insert into sys_role_permissions (role_id, permission_id)
select role.id, permission.id
from sys_roles role
join sys_permissions permission on permission.tenant_id = role.tenant_id
where role.tenant_id = 'default'
  and role.code = 'ADMIN'
  and permission.code in (
    'crm:customer:delete',
    'crm:opportunity:delete',
    'crm:quote:delete',
    'crm:contract:delete',
    'crm:followup:delete'
  )
  and not exists (
    select 1 from sys_role_permissions relation
    where relation.role_id = role.id and relation.permission_id = permission.id
  );
