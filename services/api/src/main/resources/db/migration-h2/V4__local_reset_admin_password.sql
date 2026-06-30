update sys_users
set password_hash = '{noop}Admin@123',
    updated_at = current_timestamp
where tenant_id = 'default'
  and username = 'admin';
