alter table approval_assignee_configs
  add column if not exists version_no integer not null default 1,
  add column if not exists dynamic_assignee varchar(40),
  add column if not exists auto_action varchar(20),
  add column if not exists sla_hours integer,
  add column if not exists escalation_role_id uuid;

alter table approval_assignee_configs
  add constraint fk_approval_assignee_escalation_role
  foreign key (escalation_role_id) references sys_roles(id);

alter table oa_approval_requests
  add column if not exists approval_config_version integer,
  add column if not exists approval_plan_snapshot text;
