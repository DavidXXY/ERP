CREATE TABLE procurement_supplier_categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id varchar(64) NOT NULL DEFAULT 'default',
    name varchar(80) NOT NULL,
    description varchar(240),
    sort_order integer NOT NULL DEFAULT 100,
    enabled boolean NOT NULL DEFAULT true,
    built_in boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by varchar(64),
    updated_by varchar(64),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_supplier_category_tenant_name UNIQUE (tenant_id, name)
);

WITH tenants AS (
    SELECT tenant_id FROM sys_roles
    UNION
    SELECT tenant_id FROM procurement_suppliers
    UNION
    SELECT 'default'
), common_categories(name, description, sort_order) AS (
    VALUES
      ('原材料与辅料', '生产、加工所需的原料、辅料和基础材料', 10),
      ('生产设备与机械', '生产线、机械设备及配套装置', 20),
      ('电气设备与自动化', '电气设备、控制系统和自动化产品', 30),
      ('仪器仪表', '检测、计量、实验和监测仪器', 40),
      ('IT软硬件与服务', '软件、硬件、云服务和信息技术服务', 50),
      ('工程施工', '土建、安装、装饰和专项工程施工', 60),
      ('维修维保', '设备、设施和系统的维修保养服务', 70),
      ('劳务与外包', '劳务派遣、业务外包和临时用工', 80),
      ('物流运输', '运输、配送、仓储和货运代理服务', 90),
      ('办公用品与行政物资', '办公耗材、家具及行政物资', 100),
      ('包装与耗材', '包装材料、低值易耗品和通用耗材', 110),
      ('咨询与专业服务', '咨询、审计、法务、设计和认证服务', 120),
      ('安全环保', '安防、消防、劳保和环保相关产品及服务', 130),
      ('能源与公用事业', '水、电、气、燃料和能源服务', 140),
      ('其他', '暂未归入其他类别的供应商', 999)
)
INSERT INTO procurement_supplier_categories (
    id, tenant_id, name, description, sort_order, enabled, built_in,
    created_at, updated_at, version
)
SELECT gen_random_uuid(), tenants.tenant_id, common_categories.name,
       common_categories.description, common_categories.sort_order,
       true, true, now(), now(), 0
FROM tenants
CROSS JOIN common_categories
ON CONFLICT (tenant_id, name) DO NOTHING;

INSERT INTO procurement_supplier_categories (
    id, tenant_id, name, description, sort_order, enabled, built_in,
    created_at, updated_at, version
)
SELECT gen_random_uuid(), supplier.tenant_id, btrim(supplier.category),
       '从现有供应商主档自动导入', 500, true, false, now(), now(), 0
FROM procurement_suppliers supplier
WHERE supplier.category IS NOT NULL AND btrim(supplier.category) <> ''
GROUP BY supplier.tenant_id, btrim(supplier.category)
ON CONFLICT (tenant_id, name) DO NOTHING;

CREATE INDEX idx_supplier_category_sort
    ON procurement_supplier_categories (tenant_id, enabled, sort_order, name);
CREATE INDEX IF NOT EXISTS idx_procurement_supplier_category
    ON procurement_suppliers (tenant_id, category);
