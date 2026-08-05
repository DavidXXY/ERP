ALTER TABLE fin_receivables ADD COLUMN organization_id UUID;
ALTER TABLE fin_procurement_payables ADD COLUMN organization_id UUID;

UPDATE fin_receivables receivable
SET organization_id = (
  SELECT owner_user.org_id
  FROM crm_customers customer
  JOIN sys_users owner_user ON owner_user.id = customer.owner_user_id
  WHERE customer.id = receivable.customer_id
    AND customer.tenant_id = receivable.tenant_id
  LIMIT 1
)
WHERE organization_id IS NULL;

UPDATE fin_procurement_payables payable
SET organization_id = (
  SELECT COALESCE(purchase_order.department_id, manager.org_id)
  FROM procurement_purchase_orders purchase_order
  LEFT JOIN project_projects project ON project.id = purchase_order.project_id
  LEFT JOIN sys_users manager ON manager.id = project.manager_user_id
  WHERE purchase_order.id = payable.order_id
    AND purchase_order.tenant_id = payable.tenant_id
  LIMIT 1
)
WHERE organization_id IS NULL;

ALTER TABLE fin_receivables
  ADD CONSTRAINT fk_fin_receivable_organization
  FOREIGN KEY (organization_id) REFERENCES sys_organizations(id);
ALTER TABLE fin_procurement_payables
  ADD CONSTRAINT fk_fin_payable_organization
  FOREIGN KEY (organization_id) REFERENCES sys_organizations(id);

CREATE INDEX idx_fin_receivable_organization
  ON fin_receivables (tenant_id, organization_id, due_date);
CREATE INDEX idx_fin_payable_organization
  ON fin_procurement_payables (tenant_id, organization_id, due_date);
