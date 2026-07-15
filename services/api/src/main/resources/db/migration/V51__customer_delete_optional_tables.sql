create extension if not exists pgcrypto;

create table if not exists hr_field_attendance (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  work_order_id uuid,
  employee_id uuid,
  employee_name varchar(120),
  attendance_date date,
  check_in_time timestamptz,
  check_out_time timestamptz,
  location varchar(240),
  remark varchar(500),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64)
);

create table if not exists maintenance_plans (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  contract_id uuid,
  asset_id uuid,
  plan_name varchar(180),
  status varchar(40),
  planned_date date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64)
);

create index if not exists idx_hr_field_attendance_work_order on hr_field_attendance(work_order_id);
create index if not exists idx_maintenance_plans_contract on maintenance_plans(contract_id);
create index if not exists idx_maintenance_plans_asset on maintenance_plans(asset_id);
