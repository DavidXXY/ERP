update sys_organizations
set name = '示例企业', updated_at = now()
where code = 'ROOT' and name = '工程运维公司';

update sys_permissions
set name = case code
  when 'crm:contract:view' then '客户合同查看'
  when 'inventory:view' then '库存管理查看'
  when 'inventory:part:create' then '物料新增'
  when 'maintenance:view' then '服务管理查看'
  when 'maintenance:equipment:view' then '资产设备查看'
  when 'maintenance:equipment:create' then '资产设备新增'
  when 'maintenance:plan:view' then '服务计划查看'
  when 'maintenance:plan:create' then '服务计划新增'
  when 'maintenance:plan:generate' then '服务工单生成'
  when 'maintenance:workorder:view' then '服务工单查看'
  when 'maintenance:workorder:create' then '服务工单新增'
  when 'maintenance:workorder:assign' then '服务工单派工'
  when 'maintenance:workorder:execute' then '服务工单执行'
  when 'maintenance:workorder:accept' then '服务工单验收'
  when 'workforce:view' then '人事排班查看'
  when 'workforce:schedule:create' then '排班新增'
  else name
end,
updated_at = now()
where code in (
  'crm:contract:view', 'inventory:view', 'inventory:part:create',
  'maintenance:view', 'maintenance:equipment:view', 'maintenance:equipment:create',
  'maintenance:plan:view', 'maintenance:plan:create', 'maintenance:plan:generate',
  'maintenance:workorder:view', 'maintenance:workorder:create', 'maintenance:workorder:assign',
  'maintenance:workorder:execute', 'maintenance:workorder:accept',
  'workforce:view', 'workforce:schedule:create'
);

update crm_customer_contacts
set title = '设备部', updated_at = now()
where title = '运维部';

update crm_opportunities
set need_summary = replace(replace(replace(need_summary, '驻场运维', '现场服务'), '维保', '服务'), '运维', '服务'),
    updated_at = now()
where need_summary like '%维保%' or need_summary like '%运维%';

update crm_service_contracts
set project_name = replace(replace(replace(project_name, '驻场运维', '现场服务'), '维保', '服务'), '运维', '服务'),
    contract_type = replace(replace(replace(contract_type, '驻场运维', '现场服务'), '维保', '服务'), '运维', '服务'),
    updated_at = now()
where project_name like '%维保%' or project_name like '%运维%'
   or contract_type like '%维保%' or contract_type like '%运维%';

update crm_follow_ups
set content = replace(replace(content, '驻场服务', '现场服务'), '运维', '服务'),
    updated_at = now()
where content like '%驻场%' or content like '%运维%';

update procurement_suppliers
set category = replace(category, '备件', '物料'), updated_at = now()
where category like '%备件%';

update maintenance_plans
set plan_name = replace(replace(plan_name, '周期维保', '定期服务'), '维保', '服务'),
    updated_at = now()
where plan_name like '%维保%';

update work_orders
set title = replace(replace(title, '运维工单', '服务工单'), '周期巡检', '定期检查'),
    problem_description = replace(replace(problem_description, '维保计划', '服务计划'), '周期巡检', '定期检查'),
    updated_at = now()
where title like '%运维%' or title like '%周期巡检%'
   or problem_description like '%维保%' or problem_description like '%周期巡检%';

alter table work_orders alter column title set default '服务工单';

update work_order_status_logs
set remark = replace(replace(remark, '维保计划', '服务计划'), '运维工单', '服务工单'),
    updated_at = now()
where remark like '%维保%' or remark like '%运维%';

update inventory_stock_movements
set remark = replace(replace(remark, '运维工单', '服务工单'), '备件', '物料'),
    updated_at = now()
where remark like '%运维%' or remark like '%备件%';

update fin_accounting_vouchers
set description = replace(description, '运维工单收入', '服务工单收入'),
    updated_at = now()
where description like '%运维%';

update fin_accounting_entries
set account_name = '服务收入', updated_at = now()
where account_name = '运维服务收入';

update system_notifications
set title = replace(replace(replace(title, '维保合同', '客户合同'), '设备维保', '设备服务'), '备件', '物料'),
    content = replace(replace(replace(content, '维保合同', '客户合同'), '设备维保', '设备服务'), '备件', '物料'),
    updated_at = now()
where title like '%维保%' or title like '%备件%'
   or content like '%维保%' or content like '%备件%';
