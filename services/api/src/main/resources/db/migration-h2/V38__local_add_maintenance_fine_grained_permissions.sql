merge into sys_permissions (id, tenant_id, code, name, module, created_at, updated_at) key (tenant_id, code) values
  ('00000000-0000-4000-8000-000000003801','default','maintenance:order:delete','工单删除','maintenance',current_timestamp,current_timestamp),
  ('00000000-0000-4000-8000-000000003802','default','maintenance:certificate:view','人员证书查看','maintenance',current_timestamp,current_timestamp),
  ('00000000-0000-4000-8000-000000003803','default','maintenance:schedule:view','人员排班查看','maintenance',current_timestamp,current_timestamp);

insert into sys_role_permissions (role_id, permission_id)
select role.id, permission.id
from sys_roles role
join sys_permissions permission on permission.tenant_id = role.tenant_id
where role.code = 'ADMIN'
  and permission.code in ('maintenance:order:delete','maintenance:certificate:view','maintenance:schedule:view')
  and not exists (
    select 1 from sys_role_permissions relation
    where relation.role_id = role.id and relation.permission_id = permission.id
  );

insert into sys_role_permissions (role_id, permission_id)
select role.id, permission.id
from sys_roles role
join sys_permissions permission on permission.tenant_id = role.tenant_id
where role.code = 'OPS'
  and permission.code in ('maintenance:certificate:view','maintenance:schedule:view')
  and not exists (
    select 1 from sys_role_permissions relation
    where relation.role_id = role.id and relation.permission_id = permission.id
  );
