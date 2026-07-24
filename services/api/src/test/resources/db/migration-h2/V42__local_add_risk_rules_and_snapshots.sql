create table if not exists risk_rule_configs (
  id uuid primary key,
  tenant_id varchar(64) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  created_by varchar(64),
  updated_by varchar(64),
  rule_code varchar(80) not null,
  name varchar(120) not null,
  module varchar(40) not null,
  enabled boolean not null default true,
  high_threshold numeric(18,4),
  medium_threshold numeric(18,4),
  warning_days integer,
  sla_hours integer,
  default_owner varchar(80),
  escalation_owner varchar(80),
  remark varchar(500),
  unique (tenant_id, rule_code)
);
create index if not exists idx_risk_rule_configs_module on risk_rule_configs(module, rule_code);

create table if not exists risk_snapshots (
  id uuid primary key,
  tenant_id varchar(64) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  created_by varchar(64),
  updated_by varchar(64),
  snapshot_date date not null,
  risk_key varchar(180) not null,
  module varchar(40) not null,
  module_name varchar(80) not null,
  title varchar(180) not null,
  subject varchar(300) not null,
  severity varchar(20) not null,
  status varchar(32) not null,
  workflow_status varchar(32) not null,
  amount numeric(18,2)
);
create unique index if not exists uk_risk_snapshot_tenant_date_key on risk_snapshots(tenant_id, snapshot_date, risk_key);
create index if not exists idx_risk_snapshots_date_module on risk_snapshots(snapshot_date, module);
