ALTER TABLE biz_project_handovers ADD COLUMN IF NOT EXISTS customer_contact VARCHAR(500);
ALTER TABLE biz_project_handovers ADD COLUMN IF NOT EXISTS technical_solution VARCHAR(1000);
ALTER TABLE biz_project_handovers ADD COLUMN IF NOT EXISTS quotation_summary VARCHAR(1000);
ALTER TABLE biz_project_handovers ADD COLUMN IF NOT EXISTS risk_notes VARCHAR(1000);
ALTER TABLE biz_project_staff_assignments ADD COLUMN IF NOT EXISTS allocation_percent NUMERIC(5,2) NOT NULL DEFAULT 100;

CREATE TABLE IF NOT EXISTS biz_responsibility_collaborators (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  binding_id UUID NOT NULL,
  department_id UUID NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_responsibility_collaborator UNIQUE (tenant_id, binding_id, department_id)
);

CREATE TABLE IF NOT EXISTS biz_collaboration_task_controls (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  source_type VARCHAR(40) NOT NULL,
  source_id UUID NOT NULL,
  assignee_user_id UUID,
  cc_user_ids VARCHAR(2000),
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  due_at TIMESTAMP WITH TIME ZONE,
  completed_at TIMESTAMP WITH TIME ZONE,
  last_comment VARCHAR(1000),
  reminder_count INTEGER NOT NULL DEFAULT 0,
  last_reminded_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_collaboration_task_source UNIQUE (tenant_id, source_type, source_id)
);

CREATE TABLE IF NOT EXISTS biz_collaboration_action_logs (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  source_type VARCHAR(40) NOT NULL,
  source_id UUID NOT NULL,
  action_type VARCHAR(40) NOT NULL,
  operator_user_id UUID,
  operator_name VARCHAR(80) NOT NULL,
  target_user_id UUID,
  comment VARCHAR(1000),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_project_timesheets (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  assignment_id UUID NOT NULL,
  project_id UUID NOT NULL,
  user_id UUID NOT NULL,
  work_date DATE NOT NULL,
  hours NUMERIC(8,2) NOT NULL,
  description VARCHAR(500),
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
  submitted_by UUID,
  reviewed_by UUID,
  reviewed_by_name VARCHAR(80),
  review_comment VARCHAR(500),
  reviewed_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_timesheet_period_locks (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  year_month VARCHAR(7) NOT NULL,
  locked_by UUID,
  locked_by_name VARCHAR(80) NOT NULL,
  locked_at TIMESTAMP WITH TIME ZONE NOT NULL,
  reason VARCHAR(500),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_timesheet_period_lock UNIQUE (tenant_id, year_month)
);

CREATE TABLE IF NOT EXISTS biz_project_budget_versions (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  project_id UUID NOT NULL,
  version_no INTEGER NOT NULL,
  previous_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
  requested_amount NUMERIC(14,2) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  reason VARCHAR(1000) NOT NULL,
  requested_by UUID,
  requested_by_name VARCHAR(80) NOT NULL,
  reviewed_by UUID,
  reviewed_by_name VARCHAR(80),
  review_comment VARCHAR(500),
  reviewed_at TIMESTAMP WITH TIME ZONE,
  effective_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_project_budget_version UNIQUE (tenant_id, project_id, version_no)
);

CREATE INDEX IF NOT EXISTS idx_responsibility_collab_binding ON biz_responsibility_collaborators(tenant_id, binding_id);
CREATE INDEX IF NOT EXISTS idx_collab_task_assignee ON biz_collaboration_task_controls(tenant_id, assignee_user_id, status);
CREATE INDEX IF NOT EXISTS idx_collab_action_source ON biz_collaboration_action_logs(tenant_id, source_type, source_id, created_at);
CREATE INDEX IF NOT EXISTS idx_timesheet_assignment ON biz_project_timesheets(tenant_id, assignment_id, work_date);
CREATE INDEX IF NOT EXISTS idx_timesheet_status ON biz_project_timesheets(tenant_id, status, work_date);
CREATE INDEX IF NOT EXISTS idx_budget_version_project ON biz_project_budget_versions(tenant_id, project_id, version_no);
