-- 项目风险负责人 owner_user_id：清理无效引用、按负责人姓名回填、补索引与外键（对齐 CRM V94 模式）
UPDATE project_risks
SET owner_user_id = NULL
WHERE owner_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_users u WHERE u.id = project_risks.owner_user_id);

UPDATE project_risks r
SET owner_user_id = matched.id
FROM (
  SELECT tenant_id, display_name, MIN(id::text)::uuid AS id
  FROM sys_users
  WHERE enabled = true
  GROUP BY tenant_id, display_name
) matched
WHERE r.owner_user_id IS NULL
  AND r.owner_name IS NOT NULL
  AND r.tenant_id = matched.tenant_id
  AND r.owner_name = matched.display_name;

CREATE INDEX IF NOT EXISTS idx_project_risks_owner_user
  ON project_risks (tenant_id, owner_user_id);

ALTER TABLE project_risks
  ADD CONSTRAINT fk_project_risks_owner_user FOREIGN KEY (owner_user_id) REFERENCES sys_users(id);
