create table procurement_suppliers (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  name varchar(160) not null,
  category varchar(80),
  contact_name varchar(80),
  phone varchar(40),
  settlement_terms varchar(160),
  risk_status varchar(32) not null default 'NORMAL',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table procurement_purchase_requests (
  id uuid primary key default gen_random_uuid(),
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
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table procurement_purchase_orders (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  supplier_id uuid not null references procurement_suppliers(id),
  request_id uuid references procurement_purchase_requests(id),
  order_amount numeric(14, 2) not null default 0,
  expected_delivery_date date,
  status varchar(32) not null default 'ORDERED',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create index idx_project_customer on project_projects (customer_id);
create index idx_inventory_movement_part on inventory_stock_movements (part_id);
create index idx_purchase_request_part on procurement_purchase_requests (part_id);
create index idx_purchase_order_supplier on procurement_purchase_orders (supplier_id);
create index idx_purchase_order_request on procurement_purchase_orders (request_id);

insert into project_projects (
  id, tenant_id, customer_id, code, name, stage, budget_amount, actual_cost,
  progress, warranty_end_date, created_at, updated_at
) values
(
  '44444444-1111-4111-8111-444444444444', 'default',
  '11111111-1111-4111-8111-111111111111', 'XM-2026-006',
  '虹桥车辆基地高压柜改造', 'CONSTRUCTION', 1680000, 742000, 45,
  '2027-06-30', now(), now()
),
(
  '44444444-2222-4222-8222-444444444444', 'default',
  '22222222-2222-4222-8222-222222222222', 'XM-2026-011',
  '城北泵站自控系统升级', 'COMMISSIONING', 920000, 615000, 78,
  '2027-03-31', now(), now()
),
(
  '44444444-3333-4333-8333-444444444444', 'default',
  '33333333-3333-4333-8333-333333333333', 'XM-2026-019',
  '商业广场消防联动优化', 'INITIATED', 360000, 18000, 8,
  null, now(), now()
);

insert into inventory_parts (
  id, tenant_id, code, name, model, stock_qty, safety_qty, location, unit_cost,
  created_at, updated_at
) values
(
  '55555555-1111-4111-8111-555555555555', 'default',
  'BJ-DL-001', '塑壳断路器', 'MCCB-250A', 12, 8, 'A-01-03', 680,
  now(), now()
),
(
  '55555555-2222-4222-8222-555555555555', 'default',
  'BJ-SB-014', '压力传感器', 'PT-0.6MPa', 3, 5, 'B-02-01', 430,
  now(), now()
),
(
  '55555555-3333-4333-8333-555555555555', 'default',
  'BJ-XF-028', '消防输入输出模块', 'IO-2CH', 25, 12, 'C-03-05', 95,
  now(), now()
);

insert into inventory_stock_movements (
  tenant_id, part_id, movement_type, quantity, source_no, remark, created_at, updated_at
) values
('default', '55555555-1111-4111-8111-555555555555', 'INBOUND', 12, 'INIT-STOCK', '期初库存', now(), now()),
('default', '55555555-2222-4222-8222-555555555555', 'INBOUND', 3, 'INIT-STOCK', '期初库存，低于安全库存', now(), now()),
('default', '55555555-3333-4333-8333-555555555555', 'INBOUND', 25, 'INIT-STOCK', '期初库存', now(), now());

insert into procurement_suppliers (
  id, tenant_id, code, name, category, contact_name, phone, settlement_terms,
  risk_status, created_at, updated_at
) values
(
  '66666666-1111-4111-8111-666666666666', 'default',
  'GYS-001', '上海明启电气设备有限公司', '电气备件', '陈经理',
  '13900001111', '月结30天', 'NORMAL', now(), now()
),
(
  '66666666-2222-4222-8222-666666666666', 'default',
  'GYS-002', '苏州泵控自动化有限公司', '仪表传感器', '周工',
  '13900002222', '预付30%，到货付70%', 'WATCHLIST', now(), now()
);

insert into procurement_purchase_requests (
  id, tenant_id, code, requester_name, part_id, part_name, quantity, expected_date,
  reason, status, approval_status, created_at, updated_at
) values
(
  '77777777-1111-4111-8111-777777777777', 'default',
  'CGSQ-202606-001', '仓库管理员', '55555555-2222-4222-8222-555555555555',
  '压力传感器', 10, '2026-07-05', '低于安全库存，补货用于泵站抢修',
  'SUBMITTED', 'PENDING', now(), now()
),
(
  '77777777-2222-4222-8222-777777777777', 'default',
  'CGSQ-202606-002', '项目经理A', '55555555-1111-4111-8111-555555555555',
  '塑壳断路器', 6, '2026-07-12', '虹桥车辆基地改造项目预留',
  'ORDERED', 'APPROVED', now(), now()
);

insert into procurement_purchase_orders (
  id, tenant_id, code, supplier_id, request_id, order_amount,
  expected_delivery_date, status, created_at, updated_at
) values
(
  '88888888-1111-4111-8111-888888888888', 'default',
  'CGDD-202606-001', '66666666-1111-4111-8111-666666666666',
  '77777777-2222-4222-8222-777777777777', 4080,
  '2026-07-10', 'ORDERED', now(), now()
);
