-- 历史数据回填：负责人/经办人优先取创建人或审核人，兜底取系统管理员显示名
UPDATE procurement_supplier_invoices i
  SET handler_name = u.display_name
  FROM sys_users u
  WHERE i.handler_name IS NULL AND i.created_by = u.username AND u.display_name IS NOT NULL;
UPDATE procurement_supplier_invoices i
  SET handler_name = u.display_name
  FROM sys_users u
  WHERE i.handler_name IS NULL AND u.username = 'admin' AND u.display_name IS NOT NULL;

UPDATE fin_procurement_payables p
  SET handler_name = COALESCE(
        (SELECT o.responsible_name FROM procurement_purchase_orders o WHERE o.id = p.order_id),
        (SELECT u.display_name FROM sys_users u WHERE u.username = 'admin')
      )
  WHERE p.handler_name IS NULL;

UPDATE procurement_suppliers s
  SET purchaser_name = u.display_name
  FROM sys_users u
  WHERE s.purchaser_name IS NULL AND s.created_by = u.username AND u.display_name IS NOT NULL;
UPDATE procurement_suppliers s
  SET purchaser_name = u.display_name
  FROM sys_users u
  WHERE s.purchaser_name IS NULL AND u.username = 'admin' AND u.display_name IS NOT NULL;

-- 历史订单负责人回填（演示数据）
UPDATE procurement_purchase_orders o
  SET responsible_name = u.display_name
  FROM sys_users u
  WHERE o.responsible_name IS NULL AND u.username = 'admin' AND u.display_name IS NOT NULL;
