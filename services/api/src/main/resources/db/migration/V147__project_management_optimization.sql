ALTER TABLE project_risks
  ADD COLUMN IF NOT EXISTS owner_user_id uuid;

CREATE INDEX IF NOT EXISTS idx_project_risks_due_status
  ON project_risks (tenant_id, due_date, status);

CREATE INDEX IF NOT EXISTS idx_project_milestones_planned_status
  ON project_milestones (tenant_id, planned_date, status);

CREATE INDEX IF NOT EXISTS idx_project_planned_end
  ON project_projects (tenant_id, approval_status, stage, planned_end_date);
