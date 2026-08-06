CREATE TABLE IF NOT EXISTS procurement_supplier_categories (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) DEFAULT 'default' NOT NULL,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(240),
  sort_order INTEGER DEFAULT 100 NOT NULL,
  enabled BOOLEAN DEFAULT TRUE NOT NULL,
  built_in BOOLEAN DEFAULT FALSE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_category_tenant_name
  ON procurement_supplier_categories (tenant_id, name);

INSERT INTO procurement_supplier_categories (
  id, tenant_id, name, description, sort_order, enabled, built_in,
  created_at, updated_at, version
)
SELECT RANDOM_UUID(), tenant.tenant_id, category.name, category.description,
       category.sort_order, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM (
  SELECT tenant_id FROM sys_roles
  UNION
  SELECT tenant_id FROM procurement_suppliers
  UNION
  SELECT 'default'
) tenant
CROSS JOIN (VALUES
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
) category(name, description, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM procurement_supplier_categories existing
  WHERE existing.tenant_id = tenant.tenant_id
    AND LOWER(existing.name) = LOWER(category.name)
);

INSERT INTO procurement_supplier_categories (
  id, tenant_id, name, description, sort_order, enabled, built_in,
  created_at, updated_at, version
)
SELECT RANDOM_UUID(), supplier.tenant_id, TRIM(supplier.category),
       '从现有供应商主档自动导入', 500, TRUE, FALSE,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM procurement_suppliers supplier
WHERE supplier.category IS NOT NULL
  AND TRIM(supplier.category) <> ''
  AND NOT EXISTS (
    SELECT 1 FROM procurement_supplier_categories existing
    WHERE existing.tenant_id = supplier.tenant_id
      AND LOWER(existing.name) = LOWER(TRIM(supplier.category))
  )
GROUP BY supplier.tenant_id, TRIM(supplier.category);

CREATE INDEX IF NOT EXISTS idx_supplier_category_sort
  ON procurement_supplier_categories (tenant_id, enabled, sort_order, name);
CREATE INDEX IF NOT EXISTS idx_procurement_supplier_category
  ON procurement_suppliers (tenant_id, category);
