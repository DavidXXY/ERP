ALTER TABLE project_projects
  ADD COLUMN IF NOT EXISTS actual_start_date date,
  ADD COLUMN IF NOT EXISTS actual_end_date date;

CREATE TABLE IF NOT EXISTS project_closeout_reviews (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  project_id uuid NOT NULL REFERENCES project_projects(id) ON DELETE CASCADE,
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  request_comment varchar(500),
  review_comment varchar(500),
  requested_by varchar(80),
  requested_at timestamp with time zone,
  reviewed_by varchar(80),
  reviewed_at timestamp with time zone,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_project_closeout_review
  ON project_closeout_reviews (tenant_id, project_id, created_at DESC);
