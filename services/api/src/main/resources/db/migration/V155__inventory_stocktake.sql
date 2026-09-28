-- 库存盘点：盘点单 + 盘点明细（账面/实盘/差异），过账后生成差异调整流水并更新库存

CREATE TABLE inventory_stocktakes (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  code varchar(64) NOT NULL,
  count_date date NOT NULL,
  status varchar(24) NOT NULL DEFAULT 'DRAFT',
  remark varchar(500),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_inventory_stocktakes_code ON inventory_stocktakes (tenant_id, code);

CREATE TABLE inventory_stocktake_lines (
  id uuid PRIMARY KEY,
  tenant_id varchar(64) NOT NULL,
  stocktake_id uuid NOT NULL REFERENCES inventory_stocktakes(id) ON DELETE CASCADE,
  part_id uuid NOT NULL,
  part_name varchar(160) NOT NULL,
  book_qty numeric(14,2) NOT NULL,
  actual_qty numeric(14,2),
  difference numeric(14,2),
  unit_cost numeric(14,4) NOT NULL DEFAULT 0,
  remark varchar(300),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_inventory_stocktake_lines_stocktake ON inventory_stocktake_lines (stocktake_id);

INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
VALUES
    ('00000000-0000-4000-8000-000000008101','default','inventory:stocktake:manage','库存盘点管理','inventory',now(),now(),true,0)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code IN ('ADMIN')
  AND permission.code = 'inventory:stocktake:manage'
ON CONFLICT DO NOTHING;