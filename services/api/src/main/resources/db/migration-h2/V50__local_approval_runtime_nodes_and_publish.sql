alter table approval_assignee_configs add column if not exists step_policy varchar(32) not null default 'ANY_APPROVE';
alter table approval_assignee_configs add column if not exists publish_status varchar(20) not null default 'PUBLISHED';

create table if not exists oa_approval_runtime_nodes (
  id uuid primary key,
  tenant_id varchar(64) not null default 'default',
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone,
  approval_id uuid not null,
  step_no integer not null,
  node_status varchar(32) not null default 'PENDING',
  approval_mode varchar(20),
  step_policy varchar(32) not null default 'ANY_APPROVE',
  assignee_type varchar(20),
  assignee_id uuid,
  assignee_name varchar(120),
  source_type varchar(40),
  source_value varchar(120),
  condition_text varchar(500),
  sla_hours integer,
  due_at timestamp with time zone,
  reminded_at timestamp with time zone,
  escalation_role_id uuid,
  escalated_at timestamp with time zone,
  completed_at timestamp with time zone,
  approver_id uuid,
  approver_name varchar(80),
  approval_comment varchar(500)
);
