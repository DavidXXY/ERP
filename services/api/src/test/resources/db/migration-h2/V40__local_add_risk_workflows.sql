create table if not exists risk_workflows (
  id uuid primary key,
  tenant_id varchar(64) not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  created_by varchar(64),
  updated_by varchar(64),
  risk_key varchar(180) not null,
  status varchar(32) not null default 'UNCLAIMED',
  owner varchar(80),
  note varchar(1000),
  reason varchar(500),
  updated_by_name varchar(80),
  processed_at timestamp with time zone,
  unique (tenant_id, risk_key)
);
create index if not exists idx_risk_workflows_status on risk_workflows(status, updated_at);
