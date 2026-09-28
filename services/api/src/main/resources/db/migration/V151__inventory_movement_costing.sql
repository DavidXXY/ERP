-- 库存成本核算：为库存流水补充单位成本快照与金额，支撑库存价值与销货成本口径

ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS unit_cost numeric(14,4);
ALTER TABLE inventory_stock_movements ADD COLUMN IF NOT EXISTS amount numeric(14,2);

-- 历史流水回填：单位成本取物料当前成本（近似值），金额按流水方向带符号
UPDATE inventory_stock_movements m
SET unit_cost = COALESCE(p.unit_cost, 0),
    amount = m.quantity * COALESCE(p.unit_cost, 0)
       * CASE WHEN m.movement_type IN ('OUTBOUND','SCRAP') THEN -1 ELSE 1 END
FROM inventory_parts p
WHERE m.part_id = p.id AND m.unit_cost IS NULL;

ALTER TABLE inventory_stock_movements ALTER COLUMN unit_cost SET NOT NULL;
ALTER TABLE inventory_stock_movements ALTER COLUMN unit_cost SET DEFAULT 0;
ALTER TABLE inventory_stock_movements ALTER COLUMN amount SET NOT NULL;
ALTER TABLE inventory_stock_movements ALTER COLUMN amount SET DEFAULT 0;
