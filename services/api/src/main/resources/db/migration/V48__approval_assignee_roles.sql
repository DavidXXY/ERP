alter table approval_assignee_configs add column if not exists assignee_type varchar(20) not null default 'USER';
alter table approval_assignee_configs add column if not exists role_id uuid;
alter table approval_assignee_configs alter column user_id drop not null;
alter table approval_assignee_configs drop constraint if exists uk_approval_assignee_flow_user;
alter table approval_assignee_configs add constraint ck_approval_assignee_target
  check (
    (assignee_type = 'USER' and user_id is not null and role_id is null)
    or (assignee_type = 'ROLE' and role_id is not null and user_id is null)
  );
alter table approval_assignee_configs add constraint fk_approval_assignee_role
  foreign key (role_id) references sys_roles(id);
create index if not exists idx_approval_assignee_role on approval_assignee_configs(role_id);
