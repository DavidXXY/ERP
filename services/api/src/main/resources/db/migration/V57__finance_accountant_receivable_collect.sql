INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r
JOIN sys_permissions p ON p.tenant_id = r.tenant_id
WHERE r.tenant_id = 'default'
  AND r.code = 'FINANCE_ACCOUNTANT'
  AND p.code IN ('finance:receivable:collect', 'crm:receivable:settle')
ON CONFLICT DO NOTHING;
