ALTER TABLE procurement_purchase_orders
  ADD COLUMN IF NOT EXISTS responsible_name VARCHAR(80);

UPDATE procurement_purchase_orders po
SET responsible_name = (SELECT i.created_by_name FROM procurement_inquiries i WHERE i.id = po.inquiry_id)
WHERE po.responsible_name IS NULL
  AND EXISTS (SELECT 1 FROM procurement_inquiries i WHERE i.id = po.inquiry_id AND i.created_by_name IS NOT NULL);
