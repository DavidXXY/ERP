update sys_permissions
set name = '业务待办审批查看',
    updated_at = current_timestamp
where tenant_id = 'default'
  and code = 'office:approval:view';
