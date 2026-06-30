-- [now in V1] alter table sys_organizations add column if not exists leader_name varchar(80);
-- [now in V1] alter table sys_organizations add column if not exists phone varchar(40);
-- [now in V1] alter table sys_organizations add column if not exists enabled boolean not null default true;
-- [now in V1] alter table sys_organizations add column if not exists description varchar(500);

alter table sys_roles add column if not exists built_in boolean not null default false;
alter table sys_permissions add column if not exists built_in boolean not null default false;

update sys_roles set built_in = true where code in ('ADMIN', 'CRM_MANAGER');
update sys_permissions set built_in = true;

-- [now in V1] create table if not exists sys_role_data_organizations (
  role_id uuid not null references sys_roles(id) on delete cascade,
  organization_id uuid not null references sys_organizations(id),
  primary key (role_id, organization_id)
);

update sys_organizations
set parent_id = (select id from sys_organizations where code = 'ROOT')
where code <> 'ROOT' and parent_id is null;
