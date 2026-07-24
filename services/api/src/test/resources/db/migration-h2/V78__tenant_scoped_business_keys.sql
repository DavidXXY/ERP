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
