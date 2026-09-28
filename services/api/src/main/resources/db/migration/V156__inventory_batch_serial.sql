-- 批次与序列号跟踪：库存流水增加批次/序列号字段；批次台账与序列号台账

ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS batch_no varchar(64);
ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS serial_no varchar(64);

CREATE TABLE inventory_batches (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  part_id uuid NOT NULL,
  batch_no varchar(64) NOT NULL,
  quantity numeric(14,2) NOT NULL,
  unit_cost numeric(14,4) NOT NULL,
  received_date date,
  expiry_date date,
  status varchar(16) NOT NULL DEFAULT 'ACTIVE',
  remark varchar(300),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_inventory_batches ON inventory_batches (tenant_id, part_id, batch_no);
CREATE INDEX idx_inventory_batches_part ON inventory_batches (tenant_id, part_id, status);

CREATE TABLE inventory_serial_numbers (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  part_id uuid NOT NULL,
  serial_no varchar(64) NOT NULL,
  batch_no varchar(64),
  status varchar(16) NOT NULL DEFAULT 'IN_STOCK',
  inbound_source varchar(64),
  outbound_source varchar(64),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_inventory_serial_numbers ON inventory_serial_numbers (tenant_id, part_id, serial_no);
CREATE INDEX idx_inventory_serial_numbers_part ON inventory_serial_numbers (tenant_id, part_id, status);

INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
VALUES
    ('00000000-0000-4000-8000-000000008201','default','inventory:batch:manage','批次序列号管理','inventory',now(),now(),true,0)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code IN ('ADMIN')
  AND permission.code = 'inventory:batch:manage'
ON CONFLICT DO NOTHING;