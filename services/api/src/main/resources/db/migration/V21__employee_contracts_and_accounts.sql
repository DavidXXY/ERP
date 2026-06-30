alter table qual_employees add column if not exists system_user_id uuid references sys_users(id) on delete set null;
create unique index if not exists uk_qual_employee_system_user on qual_employees (system_user_id);

create table if not exists hr_employee_contracts (
  id uuid default gen_random_uuid() primary key,
  tenant_id varchar(64) not null default 'default',
  employee_id uuid not null references qual_employees(id) on delete cascade,
  contract_no varchar(100) not null,
  contract_type varchar(80) not null,
  sign_date date,
  start_date date not null,
  end_date date,
  probation_end_date date,
  status varchar(32) not null default 'ACTIVE',
  attachments_json varchar(4000),
  remark varchar(1000),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, contract_no)
);

create index if not exists idx_employee_contract_employee on hr_employee_contracts (employee_id, end_date);
