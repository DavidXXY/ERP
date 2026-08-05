ALTER TABLE fin_receivables ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE fin_procurement_payables ADD COLUMN IF NOT EXISTS organization_id UUID;

UPDATE fin_receivables receivable
SET organization_id = owner_user.org_id
FROM crm_customers customer
JOIN sys_users owner_user
  ON owner_user.id = customer.owner_user_id
  AND owner_user.tenant_id = customer.tenant_id
WHERE receivable.customer_id = customer.id
  AND receivable.tenant_id = customer.tenant_id
  AND receivable.organization_id IS NULL;

UPDATE fin_procurement_payables payable
SET organization_id = COALESCE(purchase_order.department_id, manager.org_id)
FROM procurement_purchase_orders purchase_order
LEFT JOIN project_projects project ON project.id = purchase_order.project_id
LEFT JOIN sys_users manager ON manager.id = project.manager_user_id
WHERE payable.order_id = purchase_order.id
  AND payable.tenant_id = purchase_order.tenant_id
  AND payable.organization_id IS NULL;

ALTER TABLE fin_receivables
  ADD CONSTRAINT fk_fin_receivable_organization
  FOREIGN KEY (organization_id) REFERENCES sys_organizations(id);
ALTER TABLE fin_procurement_payables
  ADD CONSTRAINT fk_fin_payable_organization
  FOREIGN KEY (organization_id) REFERENCES sys_organizations(id);

CREATE INDEX IF NOT EXISTS idx_fin_receivable_organization
  ON fin_receivables (tenant_id, organization_id, due_date);
CREATE INDEX IF NOT EXISTS idx_fin_payable_organization
  ON fin_procurement_payables (tenant_id, organization_id, due_date);
