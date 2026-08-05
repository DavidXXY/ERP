ALTER TABLE maintenance_plans
  ADD COLUMN IF NOT EXISTS description varchar(1000),
  ADD COLUMN IF NOT EXISTS work_type varchar(80) NOT NULL DEFAULT 'INSPECTION',
  ADD COLUMN IF NOT EXISTS priority varchar(40) NOT NULL DEFAULT 'NORMAL',
  ADD COLUMN IF NOT EXISTS auto_generate boolean NOT NULL DEFAULT true;

ALTER TABLE hr_employee_certificates
  ADD COLUMN IF NOT EXISTS remark varchar(500);

ALTER TABLE hr_field_schedules
  ADD COLUMN IF NOT EXISTS work_order_id uuid REFERENCES work_orders(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS scheduled_at timestamptz;

CREATE INDEX IF NOT EXISTS idx_maintenance_plan_due
  ON maintenance_plans (tenant_id, active, auto_generate, next_due_date);

CREATE INDEX IF NOT EXISTS idx_field_schedule_user_date
  ON hr_field_schedules (tenant_id, user_id, work_date);

CREATE INDEX IF NOT EXISTS idx_field_schedule_work_order
  ON hr_field_schedules (tenant_id, work_order_id, scheduled_at DESC);

CREATE INDEX IF NOT EXISTS idx_field_attendance_user_checkin
  ON hr_field_attendance (tenant_id, user_id, check_in_at DESC);
