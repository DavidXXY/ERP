-- 创建管理员用户（原在 V26 中创建，V26 已移除演示数据）
-- 仅在用户不存在时插入
insert into sys_users (
  tenant_id, org_id, username, display_name, password_hash, enabled,
  created_at, updated_at
)
select 'default', id, 'admin', '系统管理员', '{noop}Admin@123', true,
       current_timestamp, current_timestamp
from sys_organizations
where code = 'ROOT'
  and not exists (select 1 from sys_users where username = 'admin');

-- 为 admin 分配系统管理员角色
insert into sys_user_roles (user_id, role_id)
select u.id, r.id from sys_users u, sys_roles r
where u.username = 'admin'
  and r.code in ('ADMIN', 'CRM_MANAGER')
  and not exists (
    select 1 from sys_user_roles ur
    where ur.user_id = u.id and ur.role_id = r.id
  );
