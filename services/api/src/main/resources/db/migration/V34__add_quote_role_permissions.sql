CREATE TABLE IF NOT EXISTS sys_role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_rp_role ON sys_role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_rp_perm ON sys_role_permissions(permission_id);
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sys_roles r, sys_permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('crm:quote:approve','crm:quote:customer-result','crm:quote:convert')
AND NOT EXISTS (SELECT 1 FROM sys_role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
