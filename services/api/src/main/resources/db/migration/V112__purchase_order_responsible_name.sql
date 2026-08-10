ALTER TABLE procurement_purchase_orders
  ADD COLUMN IF NOT EXISTS responsible_name VARCHAR(80);

UPDATE procurement_purchase_orders po
SET responsible_name = i.created_by_name
FROM procurement_inquiries i
WHERE po.inquiry_id = i.id
  AND po.responsible_name IS NULL
  AND i.created_by_name IS NOT NULL;
