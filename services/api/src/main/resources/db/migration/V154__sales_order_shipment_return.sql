-- 销售订单、发货、退货：销售出库扣减库存 + 销货成本凭证 + 退货入库/成本冲回/应收红冲

CREATE TABLE sales_orders (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  code varchar(64) NOT NULL,
  customer_id uuid NOT NULL,
  contract_id uuid,
  order_date date NOT NULL,
  status varchar(24) NOT NULL,
  total_amount numeric(14,2) NOT NULL DEFAULT 0,
  remark varchar(500),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sales_orders_code ON sales_orders (tenant_id, code);
CREATE INDEX idx_sales_orders_customer ON sales_orders (tenant_id, customer_id);
CREATE INDEX idx_sales_orders_contract ON sales_orders (tenant_id, contract_id);

CREATE TABLE sales_order_lines (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  order_id uuid NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
  part_id uuid NOT NULL,
  part_name varchar(160) NOT NULL,
  quantity numeric(14,2) NOT NULL,
  unit_price numeric(14,2) NOT NULL,
  amount numeric(14,2) NOT NULL,
  shipped_qty numeric(14,2) NOT NULL DEFAULT 0,
  returned_qty numeric(14,2) NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_sales_order_lines_order ON sales_order_lines (order_id);

CREATE TABLE sales_shipments (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  code varchar(64) NOT NULL,
  order_id uuid NOT NULL REFERENCES sales_orders(id) ON DELETE RESTRICT,
  customer_id uuid NOT NULL,
  contract_id uuid,
  shipment_date date NOT NULL,
  receiver_name varchar(80),
  total_amount numeric(14,2) NOT NULL DEFAULT 0,
  remark varchar(500),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sales_shipments_code ON sales_shipments (tenant_id, code);
CREATE INDEX idx_sales_shipments_order ON sales_shipments (order_id);

CREATE TABLE sales_shipment_lines (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  shipment_id uuid NOT NULL REFERENCES sales_shipments(id) ON DELETE CASCADE,
  order_line_id uuid NOT NULL REFERENCES sales_order_lines(id) ON DELETE RESTRICT,
  part_id uuid NOT NULL,
  part_name varchar(160) NOT NULL,
  quantity numeric(14,2) NOT NULL,
  unit_cost numeric(14,4) NOT NULL,
  amount numeric(14,2) NOT NULL,
  returned_qty numeric(14,2) NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_sales_shipment_lines_shipment ON sales_shipment_lines (shipment_id);

CREATE TABLE sales_return_orders (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  code varchar(64) NOT NULL,
  shipment_id uuid NOT NULL REFERENCES sales_shipments(id) ON DELETE RESTRICT,
  order_id uuid NOT NULL REFERENCES sales_orders(id) ON DELETE RESTRICT,
  customer_id uuid NOT NULL,
  contract_id uuid,
  return_date date NOT NULL,
  reason varchar(500),
  total_amount numeric(14,2) NOT NULL DEFAULT 0,
  status varchar(24) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sales_return_orders_code ON sales_return_orders (tenant_id, code);

CREATE TABLE sales_return_lines (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  return_id uuid NOT NULL REFERENCES sales_return_orders(id) ON DELETE CASCADE,
  shipment_line_id uuid NOT NULL REFERENCES sales_shipment_lines(id) ON DELETE RESTRICT,
  part_id uuid NOT NULL,
  part_name varchar(160) NOT NULL,
  quantity numeric(14,2) NOT NULL,
  unit_cost numeric(14,4) NOT NULL,
  amount numeric(14,2) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_sales_return_lines_return ON sales_return_lines (return_id);

INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
VALUES
    ('00000000-0000-4000-8000-000000008001','default','sales:order:manage','销售订单管理','sales',now(),now(),true,0),
    ('00000000-0000-4000-8000-000000008002','default','sales:shipment:manage','销售发货管理','sales',now(),now(),true,0),
    ('00000000-0000-4000-8000-000000008003','default','sales:return:manage','销售退货管理','sales',now(),now(),true,0)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code IN ('ADMIN')
  AND permission.code IN ('sales:order:manage','sales:shipment:manage','sales:return:manage')
ON CONFLICT DO NOTHING;
