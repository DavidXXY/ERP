ALTER TABLE crm_service_contracts
  ADD COLUMN IF NOT EXISTS contract_kind varchar(32) NOT NULL DEFAULT 'STANDARD',
  ADD COLUMN IF NOT EXISTS parent_contract_id uuid;

ALTER TABLE crm_service_contracts ALTER COLUMN amount DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_crm_contract_parent
  ON crm_service_contracts(parent_contract_id);

ALTER TABLE crm_service_contracts
  ADD CONSTRAINT fk_crm_contract_parent
  FOREIGN KEY (parent_contract_id) REFERENCES crm_service_contracts(id);

ALTER TABLE project_projects
  ADD COLUMN IF NOT EXISTS parent_project_id uuid;

CREATE INDEX IF NOT EXISTS idx_project_parent
  ON project_projects(parent_project_id);

ALTER TABLE project_projects
  ADD CONSTRAINT fk_project_parent
  FOREIGN KEY (parent_project_id) REFERENCES project_projects(id);
