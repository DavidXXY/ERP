CREATE TABLE procurement_material_categories (
    id uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    tenant_id varchar(64) NOT NULL DEFAULT 'default',
    name varchar(64) NOT NULL,
    built_in boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT current_timestamp,
    updated_at timestamp NOT NULL DEFAULT current_timestamp,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_material_category_tenant_name UNIQUE (tenant_id, name)
);

INSERT INTO procurement_material_categories (name, built_in)
VALUES ('工程类', true), ('服务类', true), ('劳务类', true), ('材料类', true);

ALTER TABLE inventory_parts ADD COLUMN category varchar(64);
UPDATE inventory_parts SET category = '材料类' WHERE category IS NULL;
ALTER TABLE inventory_parts ALTER COLUMN category SET NOT NULL;
ALTER TABLE inventory_parts DROP COLUMN location;

CREATE INDEX idx_inventory_parts_category
    ON inventory_parts (tenant_id, category);
