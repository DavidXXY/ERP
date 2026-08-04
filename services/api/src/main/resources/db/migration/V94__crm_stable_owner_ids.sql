ALTER TABLE crm_customers ADD COLUMN IF NOT EXISTS owner_user_id UUID;
ALTER TABLE crm_opportunities ADD COLUMN IF NOT EXISTS owner_user_id UUID;
ALTER TABLE crm_follow_ups ADD COLUMN IF NOT EXISTS owner_user_id UUID;

UPDATE crm_customers c
SET owner_user_id = matched.id
FROM (
  SELECT tenant_id, display_name, MIN(id::text)::uuid AS id
  FROM sys_users
  WHERE enabled = true
  GROUP BY tenant_id, display_name
  HAVING COUNT(*) = 1
) matched
WHERE c.owner_user_id IS NULL
  AND c.tenant_id = matched.tenant_id
  AND c.owner_name = matched.display_name;

UPDATE crm_opportunities o
SET owner_user_id = c.owner_user_id
FROM crm_customers c
WHERE o.owner_user_id IS NULL
  AND o.customer_id = c.id;

UPDATE crm_opportunities o
SET owner_user_id = matched.id
FROM (
  SELECT tenant_id, display_name, MIN(id::text)::uuid AS id
  FROM sys_users
  WHERE enabled = true
  GROUP BY tenant_id, display_name
  HAVING COUNT(*) = 1
) matched
WHERE o.owner_user_id IS NULL
  AND o.tenant_id = matched.tenant_id
  AND o.owner_name = matched.display_name;

UPDATE crm_follow_ups f
SET owner_user_id = c.owner_user_id
FROM crm_customers c
WHERE f.owner_user_id IS NULL
  AND f.customer_id = c.id;

CREATE INDEX IF NOT EXISTS idx_crm_customers_owner_user ON crm_customers (tenant_id, owner_user_id);
CREATE INDEX IF NOT EXISTS idx_crm_opportunities_owner_user ON crm_opportunities (tenant_id, owner_user_id);
CREATE INDEX IF NOT EXISTS idx_crm_follow_ups_owner_user ON crm_follow_ups (tenant_id, owner_user_id);

ALTER TABLE crm_customers
  ADD CONSTRAINT fk_crm_customers_owner_user FOREIGN KEY (owner_user_id) REFERENCES sys_users(id);
ALTER TABLE crm_opportunities
  ADD CONSTRAINT fk_crm_opportunities_owner_user FOREIGN KEY (owner_user_id) REFERENCES sys_users(id);
ALTER TABLE crm_follow_ups
  ADD CONSTRAINT fk_crm_follow_ups_owner_user FOREIGN KEY (owner_user_id) REFERENCES sys_users(id);
