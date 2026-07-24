create table if not exists approval_assignee_configs (
  id uuid primary key,
  tenant_id varchar(64) not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  created_by varchar(64),
  updated_by varchar(64),
  flow_code varchar(64) not null,
  flow_name varchar(120) not null,
  user_id uuid not null,
  approval_mode varchar(20) not null default 'PARALLEL',
  sequence_no integer not null default 1,
  enabled boolean not null default true,
  constraint fk_approval_assignee_user foreign key (user_id) references sys_users(id),
  constraint uk_approval_assignee_flow_user unique (flow_code, user_id)
);
create index if not exists idx_approval_assignee_flow on approval_assignee_configs(flow_code, enabled);
