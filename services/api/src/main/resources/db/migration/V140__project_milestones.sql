CREATE TABLE IF NOT EXISTS project_milestones (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  project_id uuid NOT NULL REFERENCES project_projects(id) ON DELETE CASCADE,
  name varchar(180) NOT NULL,
  planned_date date,
  actual_date date,
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  sort_order int NOT NULL DEFAULT 0,
  remark varchar(500),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_project_milestones_project
  ON project_milestones (tenant_id, project_id, sort_order);
