ALTER TABLE sys_organizations DROP CONSTRAINT IF EXISTS sys_organizations_code_key;
ALTER TABLE risk_rule_configs DROP CONSTRAINT IF EXISTS uks4w2e5f5yk5kaq5c8gmmj4t01;
ALTER TABLE risk_workflows DROP CONSTRAINT IF EXISTS uks225n9ue9mbt20qern0ny2ogh;
ALTER TABLE procurement_cost_allocations DROP CONSTRAINT IF EXISTS procurement_cost_allocations_receipt_id_key;
DROP INDEX IF EXISTS uk_risk_snapshots_date_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_organization_tenant_code
  ON sys_organizations (tenant_id, code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_risk_rule_tenant_code
  ON risk_rule_configs (tenant_id, rule_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_risk_workflow_tenant_key
  ON risk_workflows (tenant_id, risk_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_procurement_allocation_tenant_receipt
  ON procurement_cost_allocations (tenant_id, receipt_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_risk_snapshot_tenant_date_key
  ON risk_snapshots (tenant_id, snapshot_date, risk_key);
