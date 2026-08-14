-- 采购方确认/退回供应商发货时记录审核信息
ALTER TABLE procurement_shipments ADD COLUMN review_comment VARCHAR(500);
ALTER TABLE procurement_shipments ADD COLUMN reviewed_by VARCHAR(64);
ALTER TABLE procurement_shipments ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;
