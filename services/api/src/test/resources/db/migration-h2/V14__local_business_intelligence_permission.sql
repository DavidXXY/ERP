merge into sys_permissions(id,tenant_id,code,name,module,created_at,updated_at) key(tenant_id,code) values
('00000000-0000-4000-8000-000000003101','default','bi:view','经营分析查看','bi',current_timestamp,current_timestamp);
insert into sys_role_permissions(role_id,permission_id)
select role.id,permission.id from sys_roles role join sys_permissions permission on permission.tenant_id=role.tenant_id
where role.tenant_id='default' and role.code='ADMIN' and permission.code='bi:view'
and not exists(select 1 from sys_role_permissions relation where relation.role_id=role.id and relation.permission_id=permission.id);
