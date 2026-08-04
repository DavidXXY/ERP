ALTER TABLE crm_customers ADD COLUMN IF NOT EXISTS owner_user_id UUID;
ALTER TABLE crm_opportunities ADD COLUMN IF NOT EXISTS owner_user_id UUID;
ALTER TABLE crm_follow_ups ADD COLUMN IF NOT EXISTS owner_user_id UUID;

UPDATE crm_customers customer
SET owner_user_id = (
  SELECT user_account.id
  FROM sys_users user_account
  WHERE user_account.tenant_id = customer.tenant_id
    AND user_account.display_name = customer.owner_name
    AND user_account.enabled = true
  FETCH FIRST ROW ONLY
)
WHERE customer.owner_user_id IS NULL
  AND 1 = (
    SELECT COUNT(*)
    FROM sys_users user_account
    WHERE user_account.tenant_id = customer.tenant_id
      AND user_account.display_name = customer.owner_name
      AND user_account.enabled = true
  );

UPDATE crm_opportunities opportunity
SET owner_user_id = (
  SELECT customer.owner_user_id FROM crm_customers customer
  WHERE customer.id = opportunity.customer_id
)
WHERE opportunity.owner_user_id IS NULL;

UPDATE crm_opportunities opportunity
SET owner_user_id = (
  SELECT user_account.id
  FROM sys_users user_account
  WHERE user_account.tenant_id = opportunity.tenant_id
    AND user_account.display_name = opportunity.owner_name
    AND user_account.enabled = true
  FETCH FIRST ROW ONLY
)
WHERE opportunity.owner_user_id IS NULL
  AND 1 = (
    SELECT COUNT(*)
    FROM sys_users user_account
    WHERE user_account.tenant_id = opportunity.tenant_id
      AND user_account.display_name = opportunity.owner_name
      AND user_account.enabled = true
  );

UPDATE crm_follow_ups follow_up
SET owner_user_id = (
  SELECT customer.owner_user_id FROM crm_customers customer
  WHERE customer.id = follow_up.customer_id
)
WHERE follow_up.owner_user_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_crm_customers_owner_user ON crm_customers (tenant_id, owner_user_id);
CREATE INDEX IF NOT EXISTS idx_crm_opportunities_owner_user ON crm_opportunities (tenant_id, owner_user_id);
CREATE INDEX IF NOT EXISTS idx_crm_follow_ups_owner_user ON crm_follow_ups (tenant_id, owner_user_id);

ALTER TABLE crm_customers ADD CONSTRAINT IF NOT EXISTS fk_crm_customers_owner_user
  FOREIGN KEY (owner_user_id) REFERENCES sys_users(id);
ALTER TABLE crm_opportunities ADD CONSTRAINT IF NOT EXISTS fk_crm_opportunities_owner_user
  FOREIGN KEY (owner_user_id) REFERENCES sys_users(id);
ALTER TABLE crm_follow_ups ADD CONSTRAINT IF NOT EXISTS fk_crm_follow_ups_owner_user
  FOREIGN KEY (owner_user_id) REFERENCES sys_users(id);
