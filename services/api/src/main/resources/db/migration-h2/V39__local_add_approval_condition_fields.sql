alter table approval_assignee_configs add column if not exists condition_type varchar(32) not null default 'ANY';
alter table approval_assignee_configs add column if not exists min_amount numeric(18,2);
alter table approval_assignee_configs add column if not exists max_amount numeric(18,2);
alter table approval_assignee_configs add column if not exists department_name varchar(120);
alter table approval_assignee_configs add column if not exists remark varchar(500);
create index if not exists idx_approval_assignee_condition on approval_assignee_configs(flow_code, condition_type, enabled);
