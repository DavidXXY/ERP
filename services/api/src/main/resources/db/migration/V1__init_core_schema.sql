create extension if not exists pgcrypto;

create table sys_organizations (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null unique,
  name varchar(120) not null,
  type varchar(40) default 'DEPARTMENT',
  sort_order int default 0,
  parent_id uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64)
);

create table sys_users (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  org_id uuid references sys_organizations(id),
  username varchar(80) not null,
  display_name varchar(80) not null,
  password_hash varchar(255),
  phone varchar(40),
  email varchar(120),
  enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, username)
);

create table sys_roles (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  name varchar(120) not null,
  data_scope varchar(40) not null default 'SELF',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table sys_permissions (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  code varchar(120) not null,
  name varchar(120) not null,
  module varchar(80) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table sys_user_roles (
  user_id uuid not null references sys_users(id) on delete cascade,
  role_id uuid not null references sys_roles(id) on delete cascade,
  primary key (user_id, role_id)
);

create table crm_customers (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  name varchar(160) not null,
  industry varchar(80) not null,
  level varchar(32) not null,
  owner_name varchar(80) not null,
  payment_habit varchar(200),
  risk_status varchar(32) not null default 'NORMAL',
  risk_note varchar(500),
  invoice_title varchar(180),
  tax_no varchar(64),
  bank_name varchar(160),
  bank_account varchar(80),
  registered_address varchar(240),
  registered_phone varchar(40),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table crm_customer_contacts (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  customer_id uuid not null references crm_customers(id) on delete cascade,
  name varchar(80) not null,
  title varchar(80),
  phone varchar(40),
  email varchar(120),
  primary_contact boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64)
);

create table crm_customer_sites (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  customer_id uuid not null references crm_customers(id) on delete cascade,
  name varchar(120) not null,
  address varchar(240) not null,
  longitude numeric(10, 6),
  latitude numeric(10, 6),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64)
);

create table crm_opportunities (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  customer_id uuid references crm_customers(id),
  code varchar(64) not null,
  source varchar(80),
  need_summary varchar(500) not null,
  stage varchar(40) not null,
  expected_amount numeric(14, 2) not null default 0,
  probability int not null default 0,
  next_action varchar(240),
  next_action_at date,
  owner_name varchar(80),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table crm_quote_plans (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  customer_id uuid references crm_customers(id),
  opportunity_id uuid references crm_opportunities(id),
  code varchar(64) not null,
  service_scope varchar(800) not null,
  inspect_cycle varchar(120),
  payment_nodes varchar(300),
  amount numeric(14, 2) not null default 0,
  status varchar(40) not null default 'DRAFT',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table crm_service_contracts (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  customer_id uuid not null references crm_customers(id),
  code varchar(64) not null,
  project_name varchar(160) not null,
  contract_type varchar(80) not null,
  amount numeric(14, 2) not null default 0,
  start_date date not null,
  end_date date not null,
  service_cycle varchar(120),
  status varchar(32) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table project_projects (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  customer_id uuid references crm_customers(id),
  code varchar(64) not null,
  name varchar(180) not null,
  stage varchar(40) not null,
  budget_amount numeric(14, 2) not null default 0,
  actual_cost numeric(14, 2) not null default 0,
  progress int not null default 0,
  warranty_end_date date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table inventory_parts (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  name varchar(160) not null,
  model varchar(120),
  stock_qty numeric(14, 2) not null default 0,
  safety_qty numeric(14, 2) not null default 0,
  location varchar(80),
  unit_cost numeric(14, 2) not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table inventory_stock_movements (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  part_id uuid not null references inventory_parts(id),
  movement_type varchar(40) not null,
  quantity numeric(14, 2) not null,
  source_no varchar(64),
  remark varchar(300),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64)
);

create table work_orders (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  customer_id uuid references crm_customers(id),
  contract_id uuid references crm_service_contracts(id),
  project_id uuid references project_projects(id),
  code varchar(64) not null,
  source varchar(80) not null,
  work_type varchar(80) not null,
  priority varchar(40) not null,
  status varchar(40) not null,
  equipment_name varchar(160),
  engineer_name varchar(80),
  required_cert varchar(120),
  cost_amount numeric(14, 2) not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table fin_receivables (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  customer_id uuid not null references crm_customers(id),
  contract_id uuid references crm_service_contracts(id),
  code varchar(64) not null,
  source_no varchar(64) not null,
  amount numeric(14, 2) not null default 0,
  due_date date not null,
  status varchar(32) not null default 'INVOICE_PENDING',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table oa_approval_requests (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  approval_type varchar(80) not null,
  title varchar(180) not null,
  source_no varchar(64),
  amount numeric(14, 2),
  status varchar(40) not null default 'PENDING',
  applicant_name varchar(80),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table doc_files (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  biz_type varchar(80) not null,
  biz_id uuid,
  file_name varchar(240) not null,
  object_key varchar(500) not null,
  content_type varchar(120),
  size_bytes bigint,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64)
);

create index idx_crm_customer_name on crm_customers (tenant_id, name);
create index idx_crm_contact_customer on crm_customer_contacts (customer_id);
create index idx_crm_site_customer on crm_customer_sites (customer_id);
create index idx_contract_customer on crm_service_contracts (customer_id);
create index idx_receivable_customer on fin_receivables (customer_id);
create index idx_work_order_customer on work_orders (customer_id);

