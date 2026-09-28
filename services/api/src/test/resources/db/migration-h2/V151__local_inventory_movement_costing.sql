-- 库存成本核算（H2 测试库镜像）：生产 V151 为库存流水增加单位成本与金额

ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS unit_cost numeric(14,4) NOT NULL DEFAULT 0;
ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS amount numeric(14,2) NOT NULL DEFAULT 0;