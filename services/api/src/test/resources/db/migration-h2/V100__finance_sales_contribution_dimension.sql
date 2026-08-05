ALTER TABLE project_projects ADD COLUMN sales_owner_user_id UUID;
ALTER TABLE project_projects ADD COLUMN sales_organization_id UUID;
ALTER TABLE fin_receivables ADD COLUMN sales_owner_user_id UUID;

UPDATE project_projects project
SET sales_owner_user_id = (
      SELECT customer.owner_user_id
      FROM crm_customers customer
      WHERE customer.id = project.customer_id
        AND customer.tenant_id = project.tenant_id
      LIMIT 1
    ),
    sales_organization_id = (
      SELECT owner_user.org_id
      FROM crm_customers customer
      LEFT JOIN sys_users owner_user ON owner_user.id = customer.owner_user_id
      WHERE customer.id = project.customer_id
        AND customer.tenant_id = project.tenant_id
      LIMIT 1
    )
WHERE sales_owner_user_id IS NULL OR sales_organization_id IS NULL;

UPDATE fin_receivables receivable
SET sales_owner_user_id = (
  SELECT customer.owner_user_id
  FROM crm_customers customer
  WHERE customer.id = receivable.customer_id
    AND customer.tenant_id = receivable.tenant_id
  LIMIT 1
)
WHERE sales_owner_user_id IS NULL;

ALTER TABLE project_projects
  ADD CONSTRAINT fk_project_sales_owner
  FOREIGN KEY (sales_owner_user_id) REFERENCES sys_users(id);
ALTER TABLE project_projects
  ADD CONSTRAINT fk_project_sales_organization
  FOREIGN KEY (sales_organization_id) REFERENCES sys_organizations(id);
ALTER TABLE fin_receivables
  ADD CONSTRAINT fk_fin_receivable_sales_owner
  FOREIGN KEY (sales_owner_user_id) REFERENCES sys_users(id);

CREATE INDEX idx_project_sales_contribution
  ON project_projects (tenant_id, sales_organization_id, sales_owner_user_id);
CREATE INDEX idx_receivable_sales_owner
  ON fin_receivables (tenant_id, sales_owner_user_id, due_date);
