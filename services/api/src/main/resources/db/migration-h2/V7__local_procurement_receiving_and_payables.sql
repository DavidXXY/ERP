alter table procurement_purchase_orders
  add column if not exists part_id uuid references inventory_parts(id);

alter table procurement_purchase_orders
  add column if not exists part_name varchar(160);

alter table procurement_purchase_orders
  add column if not exists ordered_qty numeric(14, 2) not null default 0;

alter table procurement_purchase_orders
  add column if not exists received_qty numeric(14, 2) not null default 0;

alter table procurement_purchase_orders
  add column if not exists unit_price numeric(14, 2) not null default 0;

update procurement_purchase_orders orders
set part_id = (select request.part_id from procurement_purchase_requests request where request.id = orders.request_id),
    part_name = (select request.part_name from procurement_purchase_requests request where request.id = orders.request_id),
    ordered_qty = coalesce((select request.quantity from procurement_purchase_requests request where request.id = orders.request_id), 0),
    unit_price = case
      when coalesce((select request.quantity from procurement_purchase_requests request where request.id = orders.request_id), 0) > 0
      then orders.order_amount / (select request.quantity from procurement_purchase_requests request where request.id = orders.request_id)
      else 0
    end
where orders.request_id is not null
  and orders.ordered_qty = 0;

update procurement_purchase_orders
set part_name = '未关联物料'
where part_name is null;

create table if not exists procurement_request_approval_records (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  request_id uuid not null references procurement_purchase_requests(id),
  decision varchar(32) not null,
  comment varchar(500) not null,
  approver_name varchar(80) not null,
  decided_at timestamp not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64)
);

create table if not exists procurement_goods_receipts (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  order_id uuid not null references procurement_purchase_orders(id),
  part_id uuid not null references inventory_parts(id),
  quantity numeric(14, 2) not null,
  unit_price numeric(14, 2) not null,
  amount numeric(14, 2) not null,
  received_date date not null,
  delivery_no varchar(80) not null,
  receiver_name varchar(80) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table if not exists fin_procurement_payables (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  supplier_id uuid not null references procurement_suppliers(id),
  order_id uuid not null references procurement_purchase_orders(id),
  receipt_id uuid not null references procurement_goods_receipts(id),
  amount numeric(14, 2) not null,
  paid_amount numeric(14, 2) not null default 0,
  due_date date not null,
  status varchar(32) not null default 'PENDING',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create index if not exists idx_request_approval_request on procurement_request_approval_records (request_id, decided_at);
create index if not exists idx_goods_receipt_order on procurement_goods_receipts (order_id, received_date);
create index if not exists idx_procurement_payable_supplier on fin_procurement_payables (supplier_id, due_date);

merge into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) key (tenant_id, code) values
('00000000-0000-4000-8000-000000002401', 'default', 'procurement:request:approve', '采购申请审批', 'procurement', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002402', 'default', 'procurement:order:receive', '采购订单收货', 'procurement', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002403', 'default', 'procurement:payable:view', '采购应付查看', 'procurement', current_timestamp, current_timestamp);

insert into sys_role_permissions (role_id, permission_id)
select r.id, p.id
from sys_roles r
join sys_permissions p on p.tenant_id = r.tenant_id
where r.tenant_id = 'default'
  and r.code = 'ADMIN'
  and p.code in (
    'procurement:request:approve',
    'procurement:order:receive',
    'procurement:payable:view'
  )
  and not exists (
    select 1 from sys_role_permissions rp where rp.role_id = r.id and rp.permission_id = p.id
  );
