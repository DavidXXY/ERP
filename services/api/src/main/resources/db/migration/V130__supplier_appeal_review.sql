-- 供应商质检申诉：管理端处理结果（DISMISSED 维持原判 / REOPENED 打回重新质检）
ALTER TABLE procurement_goods_receipts
  ADD COLUMN IF NOT EXISTS appeal_resolution VARCHAR(32),
  ADD COLUMN IF NOT EXISTS appeal_review_comment VARCHAR(500),
  ADD COLUMN IF NOT EXISTS appeal_reviewed_by VARCHAR(64),
  ADD COLUMN IF NOT EXISTS appeal_reviewed_at TIMESTAMP WITH TIME ZONE;
