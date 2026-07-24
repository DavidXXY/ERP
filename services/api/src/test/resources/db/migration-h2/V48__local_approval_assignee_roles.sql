alter table approval_assignee_configs add column if not exists assignee_type varchar(20) not null default 'USER';
alter table approval_assignee_configs add column if not exists role_id uuid;
alter table approval_assignee_configs alter column user_id drop not null;
create index if not exists idx_approval_assignee_role on approval_assignee_configs(role_id);
