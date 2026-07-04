MERGE INTO sys_users (id, tenant_id, username, display_name, password_hash, phone, email, enabled, created_at, updated_at) KEY(tenant_id, username)
VALUES ('00000000-0000-4000-8000-000000000001', 'default', 'admin', '系统管理员',
'{bcrypt}$2a$10$uT18DXBrlWx3sQoXATT/Z.pAeSvsWPaOPtI71QLH1IVDKC9CnDqtm', '13800000000', 'admin@company.com', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at) KEY(tenant_id, code)
VALUES ('00000000-0000-4000-8000-000000000010', 'default', 'ADMIN', '系统管理员', 'ALL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO sys_user_roles (user_id, role_id) KEY(user_id, role_id)
VALUES ('00000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000010');
