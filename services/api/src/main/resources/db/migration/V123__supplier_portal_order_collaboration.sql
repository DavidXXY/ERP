-- 供应商对订单变更的回应（同意/异议）
ALTER TABLE procurement_order_changes
  ADD COLUMN IF NOT EXISTS supplier_response VARCHAR(32),
  ADD COLUMN IF NOT EXISTS supplier_comment VARCHAR(500),
  ADD COLUMN IF NOT EXISTS supplier_responded_at TIMESTAMP WITH TIME ZONE;

-- 供应商对质检结果的申诉
ALTER TABLE procurement_goods_receipts
  ADD COLUMN IF NOT EXISTS appeal_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
  ADD COLUMN IF NOT EXISTS appeal_reason VARCHAR(500),
  ADD COLUMN IF NOT EXISTS appealed_at TIMESTAMP WITH TIME ZONE;
