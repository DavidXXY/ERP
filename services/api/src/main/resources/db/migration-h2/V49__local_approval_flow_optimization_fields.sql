alter table approval_assignee_configs add column if not exists version_no integer not null default 1;
alter table approval_assignee_configs add column if not exists dynamic_assignee varchar(40);
alter table approval_assignee_configs add column if not exists auto_action varchar(20);
alter table approval_assignee_configs add column if not exists sla_hours integer;
alter table approval_assignee_configs add column if not exists escalation_role_id uuid;

alter table oa_approval_requests add column if not exists approval_config_version integer;
alter table oa_approval_requests add column if not exists approval_plan_snapshot clob;
