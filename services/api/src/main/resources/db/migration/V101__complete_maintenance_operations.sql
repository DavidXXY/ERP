ALTER TABLE maintenance_plans
  ADD COLUMN IF NOT EXISTS code varchar(64),
  ADD COLUMN IF NOT EXISTS cycle_days integer,
  ADD COLUMN IF NOT EXISTS next_due_date date,
  ADD COLUMN IF NOT EXISTS last_generated_date date,
  ADD COLUMN IF NOT EXISTS active boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS status varchar(32),
  ADD COLUMN IF NOT EXISTS planned_date date,
  ADD COLUMN IF NOT EXISTS description varchar(1000),
  ADD COLUMN IF NOT EXISTS work_type varchar(80) NOT NULL DEFAULT 'INSPECTION',
  ADD COLUMN IF NOT EXISTS priority varchar(40) NOT NULL DEFAULT 'NORMAL',
  ADD COLUMN IF NOT EXISTS auto_generate boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

-- Older production schemas used planned_date/status and allowed plan_name to be
-- null. Backfill the current maintenance-plan shape before enforcing it.
UPDATE maintenance_plans
SET code = COALESCE(code, 'MP-' || upper(substr(replace(id::text, '-', ''), 1, 12))),
    plan_name = COALESCE(NULLIF(BTRIM(plan_name), ''), '历史维保计划'),
    cycle_days = COALESCE(cycle_days, 30),
    next_due_date = COALESCE(next_due_date, planned_date, CURRENT_DATE),
    active = CASE
      WHEN status IN ('DISABLED', 'CANCELLED', 'CLOSED', 'INACTIVE') THEN false
      ELSE COALESCE(active, true)
    END;

ALTER TABLE maintenance_plans
  ALTER COLUMN code SET NOT NULL,
  ALTER COLUMN plan_name SET NOT NULL,
  ALTER COLUMN cycle_days SET NOT NULL,
  ALTER COLUMN next_due_date SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_maintenance_plans_tenant_code
  ON maintenance_plans (tenant_id, code);

ALTER TABLE hr_employee_certificates
  ADD COLUMN IF NOT EXISTS remark varchar(500);

-- Databases upgraded through the historical versioned migrations may not have
-- this table; fresh installations already receive it from B77.
CREATE TABLE IF NOT EXISTS hr_field_schedules (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  user_id uuid NOT NULL REFERENCES sys_users(id),
  work_date date NOT NULL,
  shift_name varchar(80) NOT NULL,
  site_name varchar(180),
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);

ALTER TABLE hr_field_schedules
  ADD COLUMN IF NOT EXISTS work_order_id uuid REFERENCES work_orders(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS scheduled_at timestamptz;

CREATE TABLE IF NOT EXISTS hr_field_attendance (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  user_id uuid NOT NULL,
  work_order_id uuid NOT NULL,
  check_in_at timestamptz NOT NULL,
  check_out_at timestamptz,
  check_in_location varchar(300) NOT NULL,
  check_out_location varchar(300),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);

ALTER TABLE hr_field_attendance
  ADD COLUMN IF NOT EXISTS employee_id uuid,
  ADD COLUMN IF NOT EXISTS attendance_date date,
  ADD COLUMN IF NOT EXISTS check_in_time timestamptz,
  ADD COLUMN IF NOT EXISTS check_out_time timestamptz,
  ADD COLUMN IF NOT EXISTS location varchar(300),
  ADD COLUMN IF NOT EXISTS user_id uuid,
  ADD COLUMN IF NOT EXISTS check_in_at timestamptz,
  ADD COLUMN IF NOT EXISTS check_out_at timestamptz,
  ADD COLUMN IF NOT EXISTS check_in_location varchar(300),
  ADD COLUMN IF NOT EXISTS check_out_location varchar(300),
  ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

UPDATE hr_field_attendance
SET user_id = COALESCE(user_id, employee_id),
    check_in_at = COALESCE(check_in_at, check_in_time,
      attendance_date::timestamp AT TIME ZONE 'Asia/Shanghai', created_at),
    check_out_at = COALESCE(check_out_at, check_out_time),
    check_in_location = COALESCE(NULLIF(BTRIM(check_in_location), ''),
      NULLIF(BTRIM(location), ''), '历史现场'),
    check_out_location = COALESCE(check_out_location, location);

ALTER TABLE hr_field_attendance
  ALTER COLUMN user_id SET NOT NULL,
  ALTER COLUMN work_order_id SET NOT NULL,
  ALTER COLUMN check_in_at SET NOT NULL,
  ALTER COLUMN check_in_location SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_maintenance_plan_due
  ON maintenance_plans (tenant_id, active, auto_generate, next_due_date);

CREATE INDEX IF NOT EXISTS idx_field_schedule_user_date
  ON hr_field_schedules (tenant_id, user_id, work_date);

CREATE INDEX IF NOT EXISTS idx_field_schedule_work_order
  ON hr_field_schedules (tenant_id, work_order_id, scheduled_at DESC);

CREATE INDEX IF NOT EXISTS idx_field_attendance_user_checkin
  ON hr_field_attendance (tenant_id, user_id, check_in_at DESC);
