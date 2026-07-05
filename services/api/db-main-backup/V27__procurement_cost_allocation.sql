alter table procurement_purchase_requests
  add column if not exists cost_type varchar(24) not null default 'DEPARTMENT';
alter table procurement_purchase_requests
  add column if not exists project_id uuid references project_projects(id);
alter table procurement_purchase_requests
  add column if not exists department_id uuid references sys_organizations(id);
alter table procurement_purchase_requests
  add column if not exists cost_target_code varchar(64) not null default 'PROCUREMENT_DEPARTMENT';
alter table procurement_purchase_requests
  add column if not exists cost_target_name varchar(180) not null default '采购部';

update procurement_purchase_requests
set department_id = coalesce(
      department_id,
      (select id from sys_organizations where tenant_id = 'default' and code = 'PROCUREMENT_DEPARTMENT')
    ),
    cost_target_code = 'PROCUREMENT_DEPARTMENT',
    cost_target_name = '采购部'
where cost_type = 'DEPARTMENT' and department_id is null;

alter table procurement_purchase_orders
  add column if not exists cost_type varchar(24) not null default 'DEPARTMENT';
alter table procurement_purchase_orders
  add column if not exists project_id uuid references project_projects(id);
alter table procurement_purchase_orders
  add column if not exists department_id uuid references sys_organizations(id);
alter table procurement_purchase_orders
  add column if not exists cost_target_code varchar(64) not null default 'PROCUREMENT_DEPARTMENT';
alter table procurement_purchase_orders
  add column if not exists cost_target_name varchar(180) not null default '采购部';

update procurement_purchase_orders orders
set cost_type = request.cost_type,
    project_id = request.project_id,
    department_id = request.department_id,
    cost_target_code = request.cost_target_code,
    cost_target_name = request.cost_target_name
from procurement_purchase_requests request
where orders.request_id = request.id;

create table if not exists procurement_cost_allocations (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  order_id uuid not null references procurement_purchase_orders(id),
  receipt_id uuid not null references procurement_goods_receipts(id),
  cost_type varchar(24) not null,
  project_id uuid references project_projects(id),
  department_id uuid references sys_organizations(id),
  target_code varchar(64) not null,
  target_name varchar(180) not null,
  part_name varchar(160) not null,
  amount numeric(14, 2) not null,
  incurred_date date not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (receipt_id)
);

create index if not exists idx_procurement_cost_project on procurement_cost_allocations (project_id, incurred_date desc);
create index if not exists idx_procurement_cost_department on procurement_cost_allocations (department_id, incurred_date desc);
create index if not exists idx_procurement_cost_order on procurement_cost_allocations (order_id);
