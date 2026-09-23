UPDATE project_risks
SET owner_user_id = NULL
WHERE owner_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_users u WHERE u.id = project_risks.owner_user_id);

UPDATE project_risks risk
SET owner_user_id = (
  SELECT user_account.id
  FROM sys_users user_account
  WHERE user_account.tenant_id = risk.tenant_id
    AND user_account.display_name = risk.owner_name
    AND user_account.enabled = true
  FETCH FIRST ROW ONLY
)
WHERE risk.owner_user_id IS NULL
  AND risk.owner_name IS NOT NULL
  AND 1 = (
    SELECT COUNT(*)
    FROM sys_users user_account
    WHERE user_account.tenant_id = risk.tenant_id
      AND user_account.display_name = risk.owner_name
      AND user_account.enabled = true
  );

CREATE INDEX IF NOT EXISTS idx_project_risks_owner_user ON project_risks (tenant_id, owner_user_id);

ALTER TABLE project_risks ADD CONSTRAINT IF NOT EXISTS fk_project_risks_owner_user
  FOREIGN KEY (owner_user_id) REFERENCES sys_users(id);
