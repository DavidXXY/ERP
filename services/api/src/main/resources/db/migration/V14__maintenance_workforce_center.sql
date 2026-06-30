create table if not exists maintenance_equipment_assets (
  id uuid primary key default gen_random_uuid(), tenant_id varchar(64) not null default 'default',
  customer_id uuid not null references crm_customers(id), contract_id uuid references crm_service_contracts(id),
  code varchar(64) not null, name varchar(160) not null, category varchar(80) not null,
  model varchar(120), serial_no varchar(120), site_address varchar(300) not null,
  installed_date date, warranty_end_date date, maintenance_cycle_days int not null default 90,
  last_maintenance_date date, next_maintenance_date date, status varchar(32) not null default 'ACTIVE',
  required_certificate varchar(120), notes varchar(500), created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64),
  unique (tenant_id, code)
);

create table if not exists maintenance_plans (
  id uuid primary key default gen_random_uuid(), tenant_id varchar(64) not null default 'default',
  code varchar(64) not null, asset_id uuid not null references maintenance_equipment_assets(id),
  contract_id uuid references crm_service_contracts(id), plan_name varchar(180) not null,
  cycle_days int not null, next_due_date date not null, last_generated_date date,
  active boolean not null default true, created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64),
  unique (tenant_id, code)
);

alter table work_orders add column if not exists equipment_id uuid references maintenance_equipment_assets(id);
alter table work_orders add column if not exists maintenance_plan_id uuid references maintenance_plans(id);
alter table work_orders add column if not exists title varchar(180) not null default '运维工单';
alter table work_orders add column if not exists planned_date date;
alter table work_orders add column if not exists site_address varchar(300);
alter table work_orders add column if not exists problem_description varchar(1000);
alter table work_orders add column if not exists assignee_id uuid references sys_users(id);
alter table work_orders add column if not exists check_in_at timestamptz;
alter table work_orders add column if not exists check_in_location varchar(300);
alter table work_orders add column if not exists started_at timestamptz;
alter table work_orders add column if not exists completed_at timestamptz;
alter table work_orders add column if not exists accepted_at timestamptz;
alter table work_orders add column if not exists customer_signer varchar(80);
alter table work_orders add column if not exists service_result varchar(1500);
alter table work_orders add column if not exists acceptance_note varchar(500);
alter table work_orders add column if not exists labor_hours numeric(10, 2) not null default 0;
alter table work_orders add column if not exists labor_cost numeric(14, 2) not null default 0;
alter table work_orders add column if not exists material_cost numeric(14, 2) not null default 0;
alter table work_orders add column if not exists travel_cost numeric(14, 2) not null default 0;
alter table work_orders add column if not exists outsourcing_cost numeric(14, 2) not null default 0;
alter table work_orders add column if not exists billable_amount numeric(14, 2) not null default 0;
alter table work_orders add column if not exists free_warranty boolean not null default false;

create table if not exists work_order_materials (
  id uuid primary key default gen_random_uuid(), tenant_id varchar(64) not null default 'default',
  work_order_id uuid not null references work_orders(id), part_id uuid not null references inventory_parts(id),
  part_name varchar(160) not null, quantity numeric(14, 2) not null, unit_cost numeric(14, 2) not null,
  amount numeric(14, 2) not null, created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64)
);

create table if not exists work_order_status_logs (
  id uuid primary key default gen_random_uuid(), tenant_id varchar(64) not null default 'default',
  work_order_id uuid not null references work_orders(id), from_status varchar(40), to_status varchar(40) not null,
  operator_name varchar(80) not null, remark varchar(500), created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64)
);

create table if not exists hr_employee_certificates (
  id uuid primary key default gen_random_uuid(), tenant_id varchar(64) not null default 'default',
  user_id uuid not null references sys_users(id), certificate_type varchar(120) not null,
  certificate_no varchar(120) not null, issue_date date, expiry_date date not null,
  issuing_authority varchar(180), created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64),
  unique (tenant_id, certificate_no)
);

create table if not exists hr_field_schedules (
  id uuid primary key default gen_random_uuid(), tenant_id varchar(64) not null default 'default',
  user_id uuid not null references sys_users(id), work_date date not null, shift_name varchar(80) not null,
  site_name varchar(180), status varchar(32) not null, created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64)
);

create table if not exists hr_field_attendance (
  id uuid primary key default gen_random_uuid(), tenant_id varchar(64) not null default 'default',
  user_id uuid not null references sys_users(id), work_order_id uuid not null references work_orders(id),
  check_in_at timestamptz not null, check_out_at timestamptz, check_in_location varchar(300) not null,
  check_out_location varchar(300), created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64)
);

create index if not exists idx_equipment_customer on maintenance_equipment_assets (customer_id, status);
create index if not exists idx_maintenance_plan_due on maintenance_plans (active, next_due_date);
create index if not exists idx_work_order_status_plan on work_orders (status, planned_date);
create index if not exists idx_work_order_equipment on work_orders (equipment_id, created_at desc);
create index if not exists idx_work_order_material on work_order_materials (work_order_id);
create index if not exists idx_employee_certificate_expiry on hr_employee_certificates (user_id, expiry_date);
create index if not exists idx_field_schedule_user_date on hr_field_schedules (user_id, work_date);

insert into sys_permissions (id, tenant_id, code, name, module, created_at, updated_at) values
('00000000-0000-4000-8000-000000002801', 'default', 'maintenance:view', '运维中心查看', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002802', 'default', 'maintenance:equipment:view', '设备台账查看', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002803', 'default', 'maintenance:equipment:create', '设备台账新增', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002804', 'default', 'maintenance:plan:view', '维保计划查看', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002805', 'default', 'maintenance:plan:create', '维保计划新增', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002806', 'default', 'maintenance:plan:generate', '维保工单生成', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002807', 'default', 'maintenance:workorder:view', '运维工单查看', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002808', 'default', 'maintenance:workorder:create', '运维工单新增', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002809', 'default', 'maintenance:workorder:assign', '运维工单派工', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002810', 'default', 'maintenance:workorder:execute', '运维工单执行', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002811', 'default', 'maintenance:workorder:accept', '运维工单验收', 'maintenance', now(), now()),
('00000000-0000-4000-8000-000000002812', 'default', 'workforce:view', '人员外勤查看', 'workforce', now(), now()),
('00000000-0000-4000-8000-000000002813', 'default', 'workforce:certificate:create', '人员证书新增', 'workforce', now(), now()),
('00000000-0000-4000-8000-000000002814', 'default', 'workforce:schedule:create', '外勤排班新增', 'workforce', now(), now())
on conflict (tenant_id, code) do update set name = excluded.name, module = excluded.module, updated_at = excluded.updated_at;

insert into sys_role_permissions (role_id, permission_id)
select role.id, permission.id from sys_roles role join sys_permissions permission on permission.tenant_id = role.tenant_id
where role.tenant_id = 'default' and role.code = 'ADMIN' and permission.module in ('maintenance', 'workforce')
on conflict do nothing;
