ALTER TABLE oa_expense_claims ADD COLUMN IF NOT EXISTS paid_date DATE;
ALTER TABLE oa_expense_claims ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(120);
ALTER TABLE oa_expense_claims ADD COLUMN IF NOT EXISTS paid_by_user_id UUID;
ALTER TABLE oa_expense_claims ADD COLUMN IF NOT EXISTS paid_by_name VARCHAR(80);
ALTER TABLE oa_expense_claims ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE fin_payment_applications ADD COLUMN IF NOT EXISTS applicant_user_id UUID;
ALTER TABLE fin_payment_applications ADD COLUMN IF NOT EXISTS approver_user_id UUID;
ALTER TABLE fin_payment_records ADD COLUMN IF NOT EXISTS payer_user_id UUID;

ALTER TABLE biz_accounting_periods ADD COLUMN IF NOT EXISTS pending_action VARCHAR(32);
ALTER TABLE biz_accounting_periods ADD COLUMN IF NOT EXISTS action_requested_by_id UUID;
ALTER TABLE biz_accounting_periods ADD COLUMN IF NOT EXISTS action_requested_by VARCHAR(80);
ALTER TABLE biz_accounting_periods ADD COLUMN IF NOT EXISTS action_requested_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE biz_accounting_periods ADD COLUMN IF NOT EXISTS action_request_reason VARCHAR(500);
