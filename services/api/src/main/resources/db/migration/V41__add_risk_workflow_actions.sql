create table if not exists risk_workflow_actions (
  id uuid primary key,
  tenant_id varchar(64) not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  created_by varchar(64),
  updated_by varchar(64),
  risk_key varchar(180) not null,
  from_status varchar(32),
  to_status varchar(32) not null,
  operator_name varchar(80),
  owner varchar(80),
  note varchar(1000),
  reason varchar(500)
);
create index if not exists idx_risk_workflow_actions_key_created on risk_workflow_actions(risk_key, created_at desc);
