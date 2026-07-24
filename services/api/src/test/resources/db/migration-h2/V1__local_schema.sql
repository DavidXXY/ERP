create table sys_organizations (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  name varchar(120) not null,
  type varchar(40) default 'DEPARTMENT',
  sort_order int default 0,
  parent_id uuid,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table sys_users (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  org_id uuid references sys_organizations(id),
  username varchar(80) not null,
  display_name varchar(80) not null,
  password_hash varchar(255),
  phone varchar(40),
  email varchar(120),
  enabled boolean not null default true,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, username)
);

create table sys_roles (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  name varchar(120) not null,
  data_scope varchar(40) not null default 'SELF',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table sys_permissions (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(120) not null,
  name varchar(120) not null,
  module varchar(80) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table sys_user_roles (
  user_id uuid not null references sys_users(id) on delete cascade,
  role_id uuid not null references sys_roles(id) on delete cascade,
  primary key (user_id, role_id)
);

create table sys_role_permissions (
  role_id uuid not null references sys_roles(id) on delete cascade,
  permission_id uuid not null references sys_permissions(id) on delete cascade,
  primary key (role_id, permission_id)
);

create table crm_customers (
  id uuid default RANDOM_UUID() primary key,
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
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table crm_customer_contacts (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  customer_id uuid not null references crm_customers(id) on delete cascade,
  name varchar(80) not null,
  title varchar(80),
  phone varchar(40),
  email varchar(120),
  primary_contact boolean not null default false,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64)
);

create table crm_customer_sites (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  customer_id uuid not null references crm_customers(id) on delete cascade,
  name varchar(120) not null,
  address varchar(240) not null,
  longitude numeric(10, 6),
  latitude numeric(10, 6),
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64)
);

create table crm_opportunities (
  id uuid default RANDOM_UUID() primary key,
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
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table crm_quote_plans (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  customer_id uuid references crm_customers(id),
  opportunity_id uuid references crm_opportunities(id),
  code varchar(64) not null,
  service_scope varchar(800) not null,
  inspect_cycle varchar(120),
  payment_nodes varchar(300),
  amount numeric(14, 2) not null default 0,
  status varchar(40) not null default 'DRAFT',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table crm_service_contracts (
  id uuid default RANDOM_UUID() primary key,
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
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table project_projects (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  customer_id uuid references crm_customers(id),
  code varchar(64) not null,
  name varchar(180) not null,
  stage varchar(40) not null,
  budget_amount numeric(14, 2) not null default 0,
  actual_cost numeric(14, 2) not null default 0,
  progress int not null default 0,
  warranty_end_date date,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table inventory_parts (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  name varchar(160) not null,
  model varchar(120),
  stock_qty numeric(14, 2) not null default 0,
  safety_qty numeric(14, 2) not null default 0,
  location varchar(80),
  unit_cost numeric(14, 2) not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table inventory_stock_movements (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  part_id uuid not null references inventory_parts(id),
  movement_type varchar(40) not null,
  quantity numeric(14, 2) not null,
  source_no varchar(64),
  remark varchar(300),
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64)
);

create table work_orders (
  id uuid default RANDOM_UUID() primary key,
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
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table fin_receivables (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  customer_id uuid not null references crm_customers(id),
  contract_id uuid references crm_service_contracts(id),
  code varchar(64) not null,
  source_no varchar(64) not null,
  amount numeric(14, 2) not null default 0,
  due_date date not null,
  status varchar(32) not null default 'INVOICE_PENDING',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table procurement_suppliers (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  name varchar(160) not null,
  category varchar(80),
  contact_name varchar(80),
  phone varchar(40),
  settlement_terms varchar(160),
  risk_status varchar(32) not null default 'NORMAL',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table procurement_purchase_requests (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  requester_name varchar(80) not null,
  part_id uuid references inventory_parts(id),
  part_name varchar(160) not null,
  quantity numeric(14, 2) not null default 0,
  expected_date date,
  reason varchar(300),
  status varchar(32) not null default 'SUBMITTED',
  approval_status varchar(32) not null default 'PENDING',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table procurement_purchase_orders (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  supplier_id uuid not null references procurement_suppliers(id),
  request_id uuid references procurement_purchase_requests(id),
  order_amount numeric(14, 2) not null default 0,
  expected_delivery_date date,
  status varchar(32) not null default 'ORDERED',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

-- V24 consolidated: add missing org columns
alter table sys_organizations add column if not exists leader_name varchar(80);
alter table sys_organizations add column if not exists phone varchar(40);
alter table sys_organizations add column if not exists enabled boolean not null default true;
alter table sys_organizations add column if not exists description varchar(500);

-- V24 consolidated: role data org join table
create table if not exists sys_role_data_organizations (
  role_id uuid not null references sys_roles(id) on delete cascade,
  organization_id uuid not null references sys_organizations(id),
  primary key (role_id, organization_id)
);

-- New: code sequences table
create table if not exists code_sequences (
  entity_type varchar(64) primary key,
  prefix varchar(16) not null,
  next_number bigint not null default 1
);
