CREATE TABLE IF NOT EXISTS project_risks (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  project_id uuid NOT NULL REFERENCES project_projects(id) ON DELETE CASCADE,
  title varchar(180) NOT NULL,
  description varchar(1000),
  severity varchar(16) NOT NULL DEFAULT 'MEDIUM',
  status varchar(32) NOT NULL DEFAULT 'OPEN',
  owner_name varchar(80),
  due_date date,
  resolution varchar(1000),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_project_risks_project
  ON project_risks (tenant_id, project_id, status);
