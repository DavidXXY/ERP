CREATE TABLE procurement_material_categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id varchar(64) NOT NULL DEFAULT 'default',
    name varchar(64) NOT NULL,
    built_in boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(64),
    updated_by varchar(64),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_material_category_tenant_name UNIQUE (tenant_id, name)
);

WITH tenants AS (
    SELECT tenant_id FROM sys_roles
    UNION
    SELECT tenant_id FROM inventory_parts
    UNION
    SELECT 'default'
)
INSERT INTO procurement_material_categories (
    id, tenant_id, name, built_in, created_at, updated_at, version
)
SELECT gen_random_uuid(), tenants.tenant_id, categories.name, true, now(), now(), 0
FROM tenants
CROSS JOIN (VALUES ('工程类'), ('服务类'), ('劳务类'), ('材料类')) AS categories(name)
ON CONFLICT (tenant_id, name) DO NOTHING;

ALTER TABLE inventory_parts ADD COLUMN category varchar(64);
UPDATE inventory_parts SET category = '材料类' WHERE category IS NULL OR btrim(category) = '';
ALTER TABLE inventory_parts ALTER COLUMN category SET NOT NULL;
ALTER TABLE inventory_parts DROP COLUMN location;

CREATE INDEX idx_material_category_name
    ON procurement_material_categories (tenant_id, name);
CREATE INDEX idx_inventory_parts_category
    ON inventory_parts (tenant_id, category);
