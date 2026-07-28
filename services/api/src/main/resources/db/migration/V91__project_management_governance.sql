ALTER TABLE project_projects
  ADD COLUMN IF NOT EXISTS manager_user_id uuid,
  ADD COLUMN IF NOT EXISTS manager_assigned_by_user_id uuid,
  ADD COLUMN IF NOT EXISTS manager_assigned_by_name varchar(80),
  ADD COLUMN IF NOT EXISTS manager_assigned_at timestamp with time zone,
  ADD COLUMN IF NOT EXISTS manager_assignment_comment varchar(500),
  ADD COLUMN IF NOT EXISTS approver_user_id uuid,
  ADD COLUMN IF NOT EXISTS execution_status varchar(32) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS status_comment varchar(500),
  ADD COLUMN IF NOT EXISTS status_changed_at timestamp with time zone;

UPDATE project_projects project
SET manager_user_id = (
  SELECT user_account.id
  FROM sys_users user_account
  WHERE user_account.tenant_id = project.tenant_id
    AND user_account.display_name = project.manager_name
  ORDER BY user_account.enabled DESC, user_account.created_at ASC
  LIMIT 1
)
WHERE project.manager_user_id IS NULL
  AND project.manager_name IS NOT NULL
  AND project.manager_name NOT LIKE '待%';

UPDATE project_projects
SET execution_status = CASE
  WHEN stage = 'CLOSED' THEN 'CLOSED'
  ELSE 'ACTIVE'
END,
status_changed_at = COALESCE(status_changed_at, updated_at);

ALTER TABLE project_projects
  DROP CONSTRAINT IF EXISTS project_projects_manager_user_id_fkey,
  ADD CONSTRAINT project_projects_manager_user_id_fkey
    FOREIGN KEY (manager_user_id) REFERENCES sys_users(id),
  DROP CONSTRAINT IF EXISTS project_projects_manager_assigned_by_user_id_fkey,
  ADD CONSTRAINT project_projects_manager_assigned_by_user_id_fkey
    FOREIGN KEY (manager_assigned_by_user_id) REFERENCES sys_users(id),
  DROP CONSTRAINT IF EXISTS project_projects_approver_user_id_fkey,
  ADD CONSTRAINT project_projects_approver_user_id_fkey
    FOREIGN KEY (approver_user_id) REFERENCES sys_users(id);

CREATE INDEX IF NOT EXISTS idx_project_manager_scope
  ON project_projects (tenant_id, manager_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_project_execution_stage
  ON project_projects (tenant_id, execution_status, stage, created_at DESC);

CREATE TABLE IF NOT EXISTS biz_project_budget_version_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  budget_version_id uuid NOT NULL REFERENCES biz_project_budget_versions(id) ON DELETE CASCADE,
  category varchar(40) NOT NULL,
  planned_amount numeric(14,2) NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  created_by varchar(64),
  updated_by varchar(64),
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uk_project_budget_version_item UNIQUE (tenant_id, budget_version_id, category)
);

CREATE INDEX IF NOT EXISTS idx_project_budget_version_item
  ON biz_project_budget_version_items (tenant_id, budget_version_id, category);

WITH ranked AS (
  SELECT id,
         SUM(planned_amount) OVER (PARTITION BY tenant_id, project_id, category) AS merged_amount,
         ROW_NUMBER() OVER (PARTITION BY tenant_id, project_id, category ORDER BY created_at, id) AS row_no
  FROM project_budget_items
)
UPDATE project_budget_items item
SET planned_amount = ranked.merged_amount
FROM ranked
WHERE item.id = ranked.id AND ranked.row_no = 1;

WITH ranked AS (
  SELECT id,
         ROW_NUMBER() OVER (PARTITION BY tenant_id, project_id, category ORDER BY created_at, id) AS row_no
  FROM project_budget_items
)
DELETE FROM project_budget_items item
USING ranked
WHERE item.id = ranked.id AND ranked.row_no > 1;

ALTER TABLE project_budget_items
  DROP CONSTRAINT IF EXISTS uk_project_budget_item_category,
  ADD CONSTRAINT uk_project_budget_item_category UNIQUE (tenant_id, project_id, category);

WITH ranked AS (
  SELECT id,
         ROW_NUMBER() OVER (PARTITION BY tenant_id, source_type, source_no ORDER BY updated_at DESC, id) AS row_no
  FROM project_cost_entries
  WHERE source_no IS NOT NULL AND btrim(source_no) <> ''
)
UPDATE project_cost_entries entry
SET source_no = NULL
FROM ranked
WHERE entry.id = ranked.id AND ranked.row_no > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_project_cost_source
  ON project_cost_entries (tenant_id, source_type, source_no)
  WHERE source_no IS NOT NULL AND btrim(source_no) <> '';
