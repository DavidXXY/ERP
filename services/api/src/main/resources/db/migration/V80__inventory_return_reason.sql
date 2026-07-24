ALTER TABLE inventory_return_orders
  ADD COLUMN IF NOT EXISTS reason VARCHAR(255);

UPDATE inventory_return_orders
SET reason = '历史退料记录（未填写原因）'
WHERE reason IS NULL OR BTRIM(reason) = '';

ALTER TABLE inventory_return_orders
  ALTER COLUMN reason SET NOT NULL;
