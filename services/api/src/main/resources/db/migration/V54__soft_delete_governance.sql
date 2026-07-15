CREATE TABLE IF NOT EXISTS sys_soft_delete_records (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  entity_type VARCHAR(80) NOT NULL,
  entity_id UUID NOT NULL,
  title VARCHAR(240) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  requested_by VARCHAR(120),
  requested_role_codes VARCHAR(500),
  requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  approval_id UUID,
  approved_by VARCHAR(120),
  approved_at TIMESTAMP WITH TIME ZONE,
  restored_by VARCHAR(120),
  restored_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_soft_delete_entity
  ON sys_soft_delete_records(entity_type, entity_id, status);

CREATE INDEX IF NOT EXISTS idx_soft_delete_status
  ON sys_soft_delete_records(status, requested_at);

INSERT INTO sys_permissions (id, tenant_id, code, name, module, built_in, created_at, updated_at)
SELECT '00000000-0000-4000-8000-000000002105', 'default', 'system:deleted-records:manage', '删除回收站管理', 'system', true, now(), now()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permissions WHERE tenant_id = 'default' AND code = 'system:deleted-records:manage'
);

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.tenant_id = 'default'
  AND role.code = 'ADMIN'
  AND permission.code = 'system:deleted-records:manage'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_permissions relation
    WHERE relation.role_id = role.id
      AND relation.permission_id = permission.id
  );
