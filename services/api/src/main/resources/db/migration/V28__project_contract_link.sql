alter table project_projects add column if not exists contract_id uuid references crm_service_contracts(id);
create index if not exists idx_project_contract on project_projects(contract_id);
