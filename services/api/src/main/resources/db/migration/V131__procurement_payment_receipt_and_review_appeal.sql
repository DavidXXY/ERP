-- 应付单付款回单：采购端登记付款时可上传回单附件，供应商门户可下载
ALTER TABLE fin_procurement_payables
  ADD COLUMN IF NOT EXISTS paid_at DATE,
  ADD COLUMN IF NOT EXISTS payment_note VARCHAR(500),
  ADD COLUMN IF NOT EXISTS payment_receipt_object_key VARCHAR(255),
  ADD COLUMN IF NOT EXISTS payment_receipt_file_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS payment_receipt_content_type VARCHAR(120),
  ADD COLUMN IF NOT EXISTS payment_receipt_size_bytes BIGINT,
  ADD COLUMN IF NOT EXISTS payment_receipt_uploaded_by VARCHAR(80),
  ADD COLUMN IF NOT EXISTS payment_receipt_uploaded_at TIMESTAMP WITH TIME ZONE;

-- 供应商绩效评价申诉：供应商门户发起，采购端处理（DISMISSED 维持 / REOPENED 打回修订）
ALTER TABLE procurement_supplier_reviews
  ADD COLUMN IF NOT EXISTS appeal_status VARCHAR(20) DEFAULT 'NONE',
  ADD COLUMN IF NOT EXISTS appeal_reason VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS appealed_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS appeal_resolution VARCHAR(32),
  ADD COLUMN IF NOT EXISTS appeal_review_comment VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS appeal_reviewed_by VARCHAR(80),
  ADD COLUMN IF NOT EXISTS appeal_reviewed_at TIMESTAMP WITH TIME ZONE;
