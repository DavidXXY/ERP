ALTER TABLE maintenance_plans ADD COLUMN IF NOT EXISTS code varchar(64);
ALTER TABLE maintenance_plans ADD COLUMN IF NOT EXISTS description varchar(1000);
ALTER TABLE maintenance_plans ADD COLUMN IF NOT EXISTS work_type varchar(80) DEFAULT 'INSPECTION' NOT NULL;
ALTER TABLE maintenance_plans ADD COLUMN IF NOT EXISTS priority varchar(40) DEFAULT 'NORMAL' NOT NULL;
ALTER TABLE maintenance_plans ADD COLUMN IF NOT EXISTS cycle_days integer DEFAULT 90 NOT NULL;
ALTER TABLE maintenance_plans ADD COLUMN IF NOT EXISTS next_due_date date DEFAULT current_date NOT NULL;
ALTER TABLE maintenance_plans ADD COLUMN IF NOT EXISTS last_generated_date date;
ALTER TABLE maintenance_plans ADD COLUMN IF NOT EXISTS auto_generate boolean DEFAULT true NOT NULL;
ALTER TABLE maintenance_plans ADD COLUMN IF NOT EXISTS active boolean DEFAULT true NOT NULL;
ALTER TABLE maintenance_plans ADD COLUMN IF NOT EXISTS version bigint DEFAULT 0 NOT NULL;
ALTER TABLE IF EXISTS hr_employee_certificates ADD COLUMN IF NOT EXISTS remark varchar(500);

CREATE TABLE IF NOT EXISTS hr_field_schedules (
  id uuid DEFAULT random_uuid() PRIMARY KEY,
  tenant_id varchar(64) DEFAULT 'default' NOT NULL,
  user_id uuid NOT NULL,
  work_order_id uuid,
  work_date date NOT NULL,
  scheduled_at timestamp with time zone,
  shift_name varchar(80) NOT NULL,
  site_name varchar(180),
  status varchar(32) NOT NULL,
  created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint DEFAULT 0 NOT NULL
);
ALTER TABLE hr_field_schedules ADD COLUMN IF NOT EXISTS work_order_id uuid;
ALTER TABLE hr_field_schedules ADD COLUMN IF NOT EXISTS scheduled_at timestamp with time zone;

ALTER TABLE hr_field_attendance ADD COLUMN IF NOT EXISTS user_id uuid;
ALTER TABLE hr_field_attendance ADD COLUMN IF NOT EXISTS check_in_at timestamp with time zone;
ALTER TABLE hr_field_attendance ADD COLUMN IF NOT EXISTS check_out_at timestamp with time zone;
ALTER TABLE hr_field_attendance ADD COLUMN IF NOT EXISTS check_in_location varchar(300);
ALTER TABLE hr_field_attendance ADD COLUMN IF NOT EXISTS check_out_location varchar(300);
ALTER TABLE hr_field_attendance ADD COLUMN IF NOT EXISTS version bigint DEFAULT 0 NOT NULL;
UPDATE hr_field_attendance SET user_id = employee_id WHERE user_id IS NULL AND employee_id IS NOT NULL;
UPDATE hr_field_attendance SET check_in_at = check_in_time WHERE check_in_at IS NULL AND check_in_time IS NOT NULL;
UPDATE hr_field_attendance SET check_out_at = check_out_time WHERE check_out_at IS NULL AND check_out_time IS NOT NULL;
UPDATE hr_field_attendance SET check_in_location = location WHERE check_in_location IS NULL AND location IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_maintenance_plan_due ON maintenance_plans (tenant_id, active, auto_generate, next_due_date);
CREATE INDEX IF NOT EXISTS idx_field_schedule_user_date ON hr_field_schedules (tenant_id, user_id, work_date);
CREATE INDEX IF NOT EXISTS idx_field_schedule_work_order ON hr_field_schedules (tenant_id, work_order_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_field_attendance_user_checkin ON hr_field_attendance (tenant_id, user_id, check_in_at);
