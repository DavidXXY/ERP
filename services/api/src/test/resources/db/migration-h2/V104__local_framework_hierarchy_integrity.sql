ALTER TABLE crm_service_contracts
  ADD CONSTRAINT ck_crm_contract_hierarchy_kind
  CHECK (
    (contract_kind = 'CHILD_ORDER' AND parent_contract_id IS NOT NULL)
    OR (contract_kind IN ('STANDARD', 'FRAMEWORK') AND parent_contract_id IS NULL)
  );

ALTER TABLE crm_service_contracts
  ADD CONSTRAINT ck_crm_contract_not_own_parent
  CHECK (parent_contract_id IS NULL OR parent_contract_id <> id);

ALTER TABLE project_projects
  ADD CONSTRAINT ck_project_not_own_parent
  CHECK (parent_project_id IS NULL OR parent_project_id <> id);
