-- 库存流水操作人、办公档案上传人
ALTER TABLE inventory_stock_movements
  ADD COLUMN IF NOT EXISTS operator_name VARCHAR(80);

ALTER TABLE doc_files
  ADD COLUMN IF NOT EXISTS uploaded_by VARCHAR(80);

-- 历史数据回填：优先取创建人显示名，兜底系统管理员
UPDATE inventory_stock_movements m
  SET operator_name = u.display_name
  FROM sys_users u
  WHERE m.operator_name IS NULL AND m.created_by = u.username AND u.display_name IS NOT NULL;
UPDATE inventory_stock_movements m
  SET operator_name = u.display_name
  FROM sys_users u
  WHERE m.operator_name IS NULL AND u.username = 'admin' AND u.display_name IS NOT NULL;

UPDATE doc_files d
  SET uploaded_by = u.display_name
  FROM sys_users u
  WHERE d.uploaded_by IS NULL AND d.created_by = u.username AND u.display_name IS NOT NULL;
UPDATE doc_files d
  SET uploaded_by = u.display_name
  FROM sys_users u
  WHERE d.uploaded_by IS NULL AND u.username = 'admin' AND u.display_name IS NOT NULL;
