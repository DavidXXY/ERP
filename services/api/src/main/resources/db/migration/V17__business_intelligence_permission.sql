insert into sys_permissions(id,tenant_id,code,name,module,created_at,updated_at) values
('00000000-0000-4000-8000-000000003101','default','bi:view','经营分析查看','bi',now(),now())
on conflict(tenant_id,code) do update set name=excluded.name,module=excluded.module,updated_at=excluded.updated_at;
insert into sys_role_permissions(role_id,permission_id)
select role.id,permission.id from sys_roles role join sys_permissions permission on permission.tenant_id=role.tenant_id
where role.tenant_id='default' and role.code='ADMIN' and permission.code='bi:view' on conflict do nothing;
