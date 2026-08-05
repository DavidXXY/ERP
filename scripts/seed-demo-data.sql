BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM sys_organizations
    WHERE tenant_id = 'default' AND code = 'MARKET_SALES_CENTER'
  ) THEN
    RAISE EXCEPTION 'MARKET_SALES_CENTER organization is required';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM sys_users
    WHERE tenant_id = 'default' AND username = 'test_market_sales_center'
  ) THEN
    RAISE EXCEPTION 'test_market_sales_center user is required';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM sys_users
    WHERE tenant_id = 'default' AND username = 'test_marketing_department'
  ) THEN
    RAISE EXCEPTION 'test_marketing_department user is required';
  END IF;
END
$$;

INSERT INTO crm_customers (
  id, tenant_id, code, name, industry, level, owner_name, owner_user_id,
  payment_habit, risk_status, risk_note, invoice_title, tax_no, bank_name,
  bank_account, registered_address, registered_phone, created_by, updated_by
)
VALUES
(
  'd1000000-0000-4000-8000-000000000001', 'default', 'DEMO-KH-001',
  '联城轨交运营有限公司', '轨道交通', 'STRATEGIC', '市场销售流程测试员',
  (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_market_sales_center'),
  '按里程碑付款，历史回款稳定', 'NORMAL', '年度改造预算稳定，可持续跟进扩容项目',
  '联城轨交运营有限公司', NULL, '中国建设银行南京城东支行',
  NULL, '南京市江宁区轨交大道88号', '025-8000-1001',
  'demo-seed', 'demo-seed'
),
(
  'd1000000-0000-4000-8000-000000000002', 'default', 'DEMO-KH-002',
  '澄江智慧水务有限公司', '市政水务', 'KEY', '市场销售流程测试员',
  (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_market_sales_center'),
  '月结与节点款结合', 'NORMAL', '泵站节能改造处于持续投入期',
  '澄江智慧水务有限公司', NULL, '中国工商银行无锡分行',
  NULL, '无锡市滨湖区水务路16号', '0510-8000-1002',
  'demo-seed', 'demo-seed'
),
(
  'd1000000-0000-4000-8000-000000000003', 'default', 'DEMO-KH-003',
  '海岳新能源产业园有限公司', '新能源', 'STRATEGIC', '市场部流程测试员',
  (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_marketing_department'),
  '验收后分批付款', 'NORMAL', '二期能源管理平台存在增购机会',
  '海岳新能源产业园有限公司', NULL, '中国银行苏州工业园区支行',
  NULL, '苏州市工业园区能源路66号', '0512-8000-1003',
  'demo-seed', 'demo-seed'
),
(
  'd1000000-0000-4000-8000-000000000004', 'default', 'DEMO-KH-004',
  '城南医疗中心有限公司', '医疗机构', 'KEY', '市场部流程测试员',
  (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_marketing_department'),
  '季度结算，付款及时', 'NORMAL', '重点保障双电源和应急供电稳定性',
  '城南医疗中心有限公司', NULL, '交通银行南京城南支行',
  NULL, '南京市雨花台区健康路28号', '025-8000-1004',
  'demo-seed', 'demo-seed'
)
ON CONFLICT (tenant_id, code) DO UPDATE SET
  name = EXCLUDED.name,
  industry = EXCLUDED.industry,
  level = EXCLUDED.level,
  owner_name = EXCLUDED.owner_name,
  owner_user_id = EXCLUDED.owner_user_id,
  payment_habit = EXCLUDED.payment_habit,
  risk_status = EXCLUDED.risk_status,
  risk_note = EXCLUDED.risk_note,
  tax_no = EXCLUDED.tax_no,
  bank_account = EXCLUDED.bank_account,
  updated_by = EXCLUDED.updated_by,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO crm_customer_contacts (
  id, tenant_id, customer_id, name, title, phone, email, primary_contact,
  created_by, updated_by
)
VALUES
('d1100000-0000-4000-8000-000000000001', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-001'),
 '周主任', '设备管理部主任', '13800001001', 'demo1@example.com', TRUE, 'demo-seed', 'demo-seed'),
('d1100000-0000-4000-8000-000000000002', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-002'),
 '刘经理', '生产运营部经理', '13800001002', 'demo2@example.com', TRUE, 'demo-seed', 'demo-seed'),
('d1100000-0000-4000-8000-000000000003', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-003'),
 '沈工', '能源管理负责人', '13800001003', 'demo3@example.com', TRUE, 'demo-seed', 'demo-seed'),
('d1100000-0000-4000-8000-000000000004', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-004'),
 '蒋主任', '后勤保障部主任', '13800001004', 'demo4@example.com', TRUE, 'demo-seed', 'demo-seed')
ON CONFLICT (id) DO UPDATE SET
  customer_id = EXCLUDED.customer_id,
  name = EXCLUDED.name,
  title = EXCLUDED.title,
  phone = EXCLUDED.phone,
  email = EXCLUDED.email,
  primary_contact = EXCLUDED.primary_contact,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO crm_customer_sites (
  id, tenant_id, customer_id, name, address, longitude, latitude, created_by, updated_by
)
VALUES
('d1200000-0000-4000-8000-000000000001', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-001'),
 '联城车辆基地', '南京市江宁区轨交大道88号车辆基地', 118.821000, 31.952000, 'demo-seed', 'demo-seed'),
('d1200000-0000-4000-8000-000000000002', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-002'),
 '澄江第一泵站', '无锡市滨湖区水务路16号', 120.260000, 31.510000, 'demo-seed', 'demo-seed'),
('d1200000-0000-4000-8000-000000000003', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-003'),
 '海岳能源中心', '苏州市工业园区能源路66号', 120.720000, 31.300000, 'demo-seed', 'demo-seed'),
('d1200000-0000-4000-8000-000000000004', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-004'),
 '城南医疗配电中心', '南京市雨花台区健康路28号', 118.760000, 31.980000, 'demo-seed', 'demo-seed')
ON CONFLICT (id) DO UPDATE SET
  customer_id = EXCLUDED.customer_id,
  name = EXCLUDED.name,
  address = EXCLUDED.address,
  longitude = EXCLUDED.longitude,
  latitude = EXCLUDED.latitude,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO crm_service_contracts (
  id, tenant_id, customer_id, code, project_name, contract_type, amount,
  tax_rate, start_date, end_date, service_cycle, status, created_by, updated_by
)
VALUES
('d2000000-0000-4000-8000-000000000001', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-001'),
 'DEMO-HT-001', '联城车辆基地高压柜改造', '设备改造', 1680000.00, 13.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 1, 1),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 12, 31), '按项目节点', 'ACTIVE', 'demo-seed', 'demo-seed'),
('d2000000-0000-4000-8000-000000000002', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-002'),
 'DEMO-HT-002', '澄江泵站智能节能改造', '设备改造', 960000.00, 13.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 1),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 11, 30), '按月进度', 'ACTIVE', 'demo-seed', 'demo-seed'),
('d2000000-0000-4000-8000-000000000003', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-003'),
 'DEMO-HT-003', '海岳园区综合能源管理平台', '新建项目', 2400000.00, 13.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 1, 15),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 12, 15), '按里程碑', 'ACTIVE', 'demo-seed', 'demo-seed'),
('d2000000-0000-4000-8000-000000000004', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-004'),
 'DEMO-HT-004', '城南医疗双电源保障改造', '设备改造', 720000.00, 13.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 1, 1),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 8, 31), '按验收节点', 'ACTIVE', 'demo-seed', 'demo-seed')
ON CONFLICT (tenant_id, code) DO UPDATE SET
  customer_id = EXCLUDED.customer_id,
  project_name = EXCLUDED.project_name,
  contract_type = EXCLUDED.contract_type,
  amount = EXCLUDED.amount,
  tax_rate = EXCLUDED.tax_rate,
  start_date = EXCLUDED.start_date,
  end_date = EXCLUDED.end_date,
  service_cycle = EXCLUDED.service_cycle,
  status = EXCLUDED.status,
  updated_by = EXCLUDED.updated_by,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO project_projects (
  id, tenant_id, customer_id, contract_id, code, name, project_type, stage,
  budget_amount, actual_cost, contract_amount, progress, warranty_end_date,
  planned_start_date, planned_end_date, manager_name, manager_user_id,
  site_address, approval_status, approver_name, approved_at, execution_status,
  sales_owner_user_id, sales_organization_id, created_by, updated_by
)
VALUES
('d3000000-0000-4000-8000-000000000001', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-001'),
 (SELECT id FROM crm_service_contracts WHERE tenant_id = 'default' AND code = 'DEMO-HT-001'),
 'DEMO-XM-001', '联城车辆基地高压柜改造', 'RENOVATION', 'CONSTRUCTION',
 920000.00, 780000.00, 1680000.00, 68,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER + 1, 6, 30),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 1, 5),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 10, 31), '项目管理流程测试员',
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_project_management_department'),
 '南京市江宁区联城车辆基地', 'APPROVED', '系统管理员', CURRENT_TIMESTAMP, 'ACTIVE',
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_market_sales_center'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKET_SALES_CENTER'),
 'demo-seed', 'demo-seed'),
('d3000000-0000-4000-8000-000000000002', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-002'),
 (SELECT id FROM crm_service_contracts WHERE tenant_id = 'default' AND code = 'DEMO-HT-002'),
 'DEMO-XM-002', '澄江泵站智能节能改造', 'O_M_RENOVATION', 'COMMISSIONING',
 580000.00, 500000.00, 960000.00, 82,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER + 1, 5, 31),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 10),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 9, 30), '项目管理流程测试员',
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_project_management_department'),
 '无锡市滨湖区澄江第一泵站', 'APPROVED', '系统管理员', CURRENT_TIMESTAMP, 'ACTIVE',
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_market_sales_center'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKET_SALES_CENTER'),
 'demo-seed', 'demo-seed'),
('d3000000-0000-4000-8000-000000000003', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-003'),
 (SELECT id FROM crm_service_contracts WHERE tenant_id = 'default' AND code = 'DEMO-HT-003'),
 'DEMO-XM-003', '海岳园区综合能源管理平台', 'NEW_CONSTRUCTION', 'CONSTRUCTION',
 1380000.00, 1230000.00, 2400000.00, 55,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER + 1, 12, 31),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 1, 20),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 12, 15), '项目管理流程测试员',
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_project_management_department'),
 '苏州市工业园区海岳能源中心', 'APPROVED', '系统管理员', CURRENT_TIMESTAMP, 'ACTIVE',
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_marketing_department'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKETING_DEPARTMENT'),
 'demo-seed', 'demo-seed'),
('d3000000-0000-4000-8000-000000000004', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-004'),
 (SELECT id FROM crm_service_contracts WHERE tenant_id = 'default' AND code = 'DEMO-HT-004'),
 'DEMO-XM-004', '城南医疗双电源保障改造', 'RENOVATION', 'FINAL_ACCEPTANCE',
 430000.00, 360000.00, 720000.00, 100,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER + 1, 8, 31),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 1, 10),
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 7, 31), '项目管理流程测试员',
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_project_management_department'),
 '南京市雨花台区城南医疗中心', 'APPROVED', '系统管理员', CURRENT_TIMESTAMP, 'ACTIVE',
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_marketing_department'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKETING_DEPARTMENT'),
 'demo-seed', 'demo-seed')
ON CONFLICT (tenant_id, code) DO UPDATE SET
  customer_id = EXCLUDED.customer_id,
  contract_id = EXCLUDED.contract_id,
  name = EXCLUDED.name,
  project_type = EXCLUDED.project_type,
  stage = EXCLUDED.stage,
  budget_amount = EXCLUDED.budget_amount,
  actual_cost = EXCLUDED.actual_cost,
  contract_amount = EXCLUDED.contract_amount,
  progress = EXCLUDED.progress,
  manager_name = EXCLUDED.manager_name,
  manager_user_id = EXCLUDED.manager_user_id,
  site_address = EXCLUDED.site_address,
  approval_status = EXCLUDED.approval_status,
  execution_status = EXCLUDED.execution_status,
  sales_owner_user_id = EXCLUDED.sales_owner_user_id,
  sales_organization_id = EXCLUDED.sales_organization_id,
  updated_by = EXCLUDED.updated_by,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO project_cost_entries (
  id, tenant_id, project_id, category, source_type, source_no, description,
  amount, incurred_date, created_by, updated_by
)
VALUES
('d4000000-0000-4000-8000-000000000001', 'default',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-001'),
 'MATERIAL', 'PROCUREMENT', 'DEMO-COST-001-M', '高压柜及保护装置采购', 520000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 3, 5), 'demo-seed', 'demo-seed'),
('d4000000-0000-4000-8000-000000000002', 'default',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-001'),
 'LABOR', 'MANUAL', 'DEMO-COST-001-L', '现场施工及调试人工', 260000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 4, 18), 'demo-seed', 'demo-seed'),
('d4000000-0000-4000-8000-000000000003', 'default',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-002'),
 'MATERIAL', 'PROCUREMENT', 'DEMO-COST-002-M', '泵站传感与变频设备采购', 310000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 3, 20), 'demo-seed', 'demo-seed'),
('d4000000-0000-4000-8000-000000000004', 'default',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-002'),
 'LABOR', 'MANUAL', 'DEMO-COST-002-L', '系统集成与试运行人工', 190000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 5, 12), 'demo-seed', 'demo-seed'),
('d4000000-0000-4000-8000-000000000005', 'default',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-003'),
 'MATERIAL', 'PROCUREMENT', 'DEMO-COST-003-M', '能源管理平台硬件采购', 820000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 8), 'demo-seed', 'demo-seed'),
('d4000000-0000-4000-8000-000000000006', 'default',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-003'),
 'SUBCONTRACT', 'SUBCONTRACT', 'DEMO-COST-003-S', '平台部署与专业分包', 410000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 6, 10), 'demo-seed', 'demo-seed'),
('d4000000-0000-4000-8000-000000000007', 'default',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-004'),
 'MATERIAL', 'PROCUREMENT', 'DEMO-COST-004-M', '双电源切换柜采购', 210000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 20), 'demo-seed', 'demo-seed'),
('d4000000-0000-4000-8000-000000000008', 'default',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-004'),
 'LABOR', 'MANUAL', 'DEMO-COST-004-L', '安装调试及应急演练', 150000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 4, 6), 'demo-seed', 'demo-seed')
ON CONFLICT (id) DO UPDATE SET
  project_id = EXCLUDED.project_id,
  category = EXCLUDED.category,
  source_type = EXCLUDED.source_type,
  source_no = EXCLUDED.source_no,
  description = EXCLUDED.description,
  amount = EXCLUDED.amount,
  incurred_date = EXCLUDED.incurred_date,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO fin_receivables (
  id, tenant_id, customer_id, contract_id, code, source_no, amount, due_date,
  status, invoice_no, invoice_date, settled_amount, invoice_requested,
  invoice_request_status, organization_id, sales_owner_user_id, created_by, updated_by
)
VALUES
('d5000000-0000-4000-8000-000000000001', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-001'),
 (SELECT id FROM crm_service_contracts WHERE tenant_id = 'default' AND code = 'DEMO-HT-001'),
 'DEMO-YS-001', 'DEMO-HT-001', 1680000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 9, 30), 'PAYMENT_PENDING',
 'DEMO-FP-001', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 1), 1100000.00,
 TRUE, 'APPROVED',
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKET_SALES_CENTER'),
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_market_sales_center'),
 'demo-seed', 'demo-seed'),
('d5000000-0000-4000-8000-000000000002', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-002'),
 (SELECT id FROM crm_service_contracts WHERE tenant_id = 'default' AND code = 'DEMO-HT-002'),
 'DEMO-YS-002', 'DEMO-HT-002', 960000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 8, 31), 'PAYMENT_PENDING',
 'DEMO-FP-002', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 3, 1), 600000.00,
 TRUE, 'APPROVED',
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKET_SALES_CENTER'),
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_market_sales_center'),
 'demo-seed', 'demo-seed'),
('d5000000-0000-4000-8000-000000000003', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-003'),
 (SELECT id FROM crm_service_contracts WHERE tenant_id = 'default' AND code = 'DEMO-HT-003'),
 'DEMO-YS-003', 'DEMO-HT-003', 2400000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 10, 31), 'PAYMENT_PENDING',
 'DEMO-FP-003', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 1, 20), 1700000.00,
 TRUE, 'APPROVED',
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKETING_DEPARTMENT'),
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_marketing_department'),
 'demo-seed', 'demo-seed'),
('d5000000-0000-4000-8000-000000000004', 'default',
 (SELECT id FROM crm_customers WHERE tenant_id = 'default' AND code = 'DEMO-KH-004'),
 (SELECT id FROM crm_service_contracts WHERE tenant_id = 'default' AND code = 'DEMO-HT-004'),
 'DEMO-YS-004', 'DEMO-HT-004', 720000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 7, 31), 'SETTLED',
 'DEMO-FP-004', make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 10), 720000.00,
 TRUE, 'APPROVED',
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKETING_DEPARTMENT'),
 (SELECT id FROM sys_users WHERE tenant_id = 'default' AND username = 'test_marketing_department'),
 'demo-seed', 'demo-seed')
ON CONFLICT (tenant_id, code) DO UPDATE SET
  customer_id = EXCLUDED.customer_id,
  contract_id = EXCLUDED.contract_id,
  source_no = EXCLUDED.source_no,
  amount = EXCLUDED.amount,
  due_date = EXCLUDED.due_date,
  status = EXCLUDED.status,
  invoice_no = EXCLUDED.invoice_no,
  invoice_date = EXCLUDED.invoice_date,
  settled_amount = EXCLUDED.settled_amount,
  invoice_requested = EXCLUDED.invoice_requested,
  invoice_request_status = EXCLUDED.invoice_request_status,
  organization_id = EXCLUDED.organization_id,
  sales_owner_user_id = EXCLUDED.sales_owner_user_id,
  updated_by = EXCLUDED.updated_by,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO fin_receivable_receipts (
  id, tenant_id, receivable_id, amount, received_date, reference_no,
  recorder_name, created_by, updated_by
)
VALUES
('d6000000-0000-4000-8000-000000000001', 'default',
 (SELECT id FROM fin_receivables WHERE tenant_id = 'default' AND code = 'DEMO-YS-001'),
 600000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 15), 'DEMO-HK-001-1', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('d6000000-0000-4000-8000-000000000002', 'default',
 (SELECT id FROM fin_receivables WHERE tenant_id = 'default' AND code = 'DEMO-YS-001'),
 500000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 4, 20), 'DEMO-HK-001-2', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('d6000000-0000-4000-8000-000000000003', 'default',
 (SELECT id FROM fin_receivables WHERE tenant_id = 'default' AND code = 'DEMO-YS-002'),
 300000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 3, 10), 'DEMO-HK-002-1', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('d6000000-0000-4000-8000-000000000004', 'default',
 (SELECT id FROM fin_receivables WHERE tenant_id = 'default' AND code = 'DEMO-YS-002'),
 300000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 7, 18), 'DEMO-HK-002-2', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('d6000000-0000-4000-8000-000000000005', 'default',
 (SELECT id FROM fin_receivables WHERE tenant_id = 'default' AND code = 'DEMO-YS-003'),
 700000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 1, 25), 'DEMO-HK-003-1', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('d6000000-0000-4000-8000-000000000006', 'default',
 (SELECT id FROM fin_receivables WHERE tenant_id = 'default' AND code = 'DEMO-YS-003'),
 600000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 5, 16), 'DEMO-HK-003-2', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('d6000000-0000-4000-8000-000000000007', 'default',
 (SELECT id FROM fin_receivables WHERE tenant_id = 'default' AND code = 'DEMO-YS-003'),
 400000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 8, 1), 'DEMO-HK-003-3', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('d6000000-0000-4000-8000-000000000008', 'default',
 (SELECT id FROM fin_receivables WHERE tenant_id = 'default' AND code = 'DEMO-YS-004'),
 400000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 28), 'DEMO-HK-004-1', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('d6000000-0000-4000-8000-000000000009', 'default',
 (SELECT id FROM fin_receivables WHERE tenant_id = 'default' AND code = 'DEMO-YS-004'),
 320000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 6, 12), 'DEMO-HK-004-2', '出纳流程测试员', 'demo-seed', 'demo-seed')
ON CONFLICT (tenant_id, reference_no) DO UPDATE SET
  receivable_id = EXCLUDED.receivable_id,
  amount = EXCLUDED.amount,
  received_date = EXCLUDED.received_date,
  recorder_name = EXCLUDED.recorder_name,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO procurement_suppliers (
  id, tenant_id, code, name, category, contact_name, phone, settlement_terms,
  risk_status, admission_status, business_scope, taxpayer_type, created_by, updated_by
)
VALUES (
  'd7000000-0000-4000-8000-000000000001', 'default', 'DEMO-GYS-001',
  '江苏智联电气设备有限公司', '电气设备与自动化', '陈经理', '13900002001',
  '到货验收后60天', 'NORMAL', 'APPROVED', '高低压成套设备、自动化控制及能源管理硬件',
  '一般纳税人', 'demo-seed', 'demo-seed'
)
ON CONFLICT (tenant_id, code) DO UPDATE SET
  name = EXCLUDED.name,
  category = EXCLUDED.category,
  contact_name = EXCLUDED.contact_name,
  phone = EXCLUDED.phone,
  settlement_terms = EXCLUDED.settlement_terms,
  risk_status = EXCLUDED.risk_status,
  admission_status = EXCLUDED.admission_status,
  business_scope = EXCLUDED.business_scope,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO inventory_parts (
  id, tenant_id, code, name, model, category, stock_qty, safety_qty, unit_cost,
  created_by, updated_by
)
VALUES (
  'd8000000-0000-4000-8000-000000000001', 'default', 'DEMO-WL-001',
  '智能配电采集网关', 'E-GATEWAY-4G', '智能电气', 18.00, 6.00, 12800.00,
  'demo-seed', 'demo-seed'
)
ON CONFLICT (tenant_id, code) DO UPDATE SET
  name = EXCLUDED.name,
  model = EXCLUDED.model,
  category = EXCLUDED.category,
  stock_qty = EXCLUDED.stock_qty,
  safety_qty = EXCLUDED.safety_qty,
  unit_cost = EXCLUDED.unit_cost,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO inventory_stock_movements (
  id, tenant_id, part_id, movement_type, quantity, source_no, remark, created_by, updated_by
)
VALUES (
  'd8100000-0000-4000-8000-000000000001', 'default',
  (SELECT id FROM inventory_parts WHERE tenant_id = 'default' AND code = 'DEMO-WL-001'),
  'INBOUND', 18.00, 'DEMO-INIT-STOCK', '演示数据期初入库', 'demo-seed', 'demo-seed'
)
ON CONFLICT (id) DO UPDATE SET
  part_id = EXCLUDED.part_id,
  quantity = EXCLUDED.quantity,
  remark = EXCLUDED.remark,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO procurement_purchase_orders (
  id, tenant_id, code, supplier_id, part_id, part_name, ordered_qty, received_qty,
  unit_price, tax_rate, order_amount, expected_delivery_date, cost_type, project_id,
  department_id, cost_target_code, cost_target_name, status, approval_status,
  approval_comment, approver_name, approved_at, currency, freight_amount,
  source_reason, submitted_at, created_by, updated_by
)
VALUES
('d9000000-0000-4000-8000-000000000001', 'default', 'DEMO-CGD-001',
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 (SELECT id FROM inventory_parts WHERE tenant_id = 'default' AND code = 'DEMO-WL-001'),
 '高压柜及保护装置', 1.00, 1.00, 520000.00, 13.00, 520000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 3, 1), 'PROJECT',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-001'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKET_SALES_CENTER'),
 'DEMO-XM-001', '联城车辆基地高压柜改造', 'RECEIVED', 'APPROVED', '演示采购审批通过',
 '系统管理员', CURRENT_TIMESTAMP, 'CNY', 0, '项目主设备采购', CURRENT_TIMESTAMP,
 'demo-seed', 'demo-seed'),
('d9000000-0000-4000-8000-000000000002', 'default', 'DEMO-CGD-002',
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 (SELECT id FROM inventory_parts WHERE tenant_id = 'default' AND code = 'DEMO-WL-001'),
 '泵站传感与变频设备', 1.00, 1.00, 310000.00, 13.00, 310000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 3, 15), 'PROJECT',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-002'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKET_SALES_CENTER'),
 'DEMO-XM-002', '澄江泵站智能节能改造', 'RECEIVED', 'APPROVED', '演示采购审批通过',
 '系统管理员', CURRENT_TIMESTAMP, 'CNY', 0, '项目传感和变频设备采购', CURRENT_TIMESTAMP,
 'demo-seed', 'demo-seed'),
('d9000000-0000-4000-8000-000000000003', 'default', 'DEMO-CGD-003',
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 (SELECT id FROM inventory_parts WHERE tenant_id = 'default' AND code = 'DEMO-WL-001'),
 '能源管理平台硬件', 1.00, 1.00, 820000.00, 13.00, 820000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 5), 'PROJECT',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-003'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKETING_DEPARTMENT'),
 'DEMO-XM-003', '海岳园区综合能源管理平台', 'RECEIVED', 'APPROVED', '演示采购审批通过',
 '系统管理员', CURRENT_TIMESTAMP, 'CNY', 0, '平台硬件和边缘采集设备采购', CURRENT_TIMESTAMP,
 'demo-seed', 'demo-seed'),
('d9000000-0000-4000-8000-000000000004', 'default', 'DEMO-CGD-004',
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 (SELECT id FROM inventory_parts WHERE tenant_id = 'default' AND code = 'DEMO-WL-001'),
 '双电源切换柜', 1.00, 1.00, 210000.00, 13.00, 210000.00,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 18), 'PROJECT',
 (SELECT id FROM project_projects WHERE tenant_id = 'default' AND code = 'DEMO-XM-004'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKETING_DEPARTMENT'),
 'DEMO-XM-004', '城南医疗双电源保障改造', 'RECEIVED', 'APPROVED', '演示采购审批通过',
 '系统管理员', CURRENT_TIMESTAMP, 'CNY', 0, '双电源核心设备采购', CURRENT_TIMESTAMP,
 'demo-seed', 'demo-seed')
ON CONFLICT (tenant_id, code) DO UPDATE SET
  supplier_id = EXCLUDED.supplier_id,
  part_id = EXCLUDED.part_id,
  part_name = EXCLUDED.part_name,
  ordered_qty = EXCLUDED.ordered_qty,
  received_qty = EXCLUDED.received_qty,
  unit_price = EXCLUDED.unit_price,
  tax_rate = EXCLUDED.tax_rate,
  order_amount = EXCLUDED.order_amount,
  cost_type = EXCLUDED.cost_type,
  project_id = EXCLUDED.project_id,
  department_id = EXCLUDED.department_id,
  cost_target_code = EXCLUDED.cost_target_code,
  cost_target_name = EXCLUDED.cost_target_name,
  status = EXCLUDED.status,
  approval_status = EXCLUDED.approval_status,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO procurement_goods_receipts (
  id, tenant_id, code, order_id, part_id, quantity, unit_price, amount,
  received_date, delivery_no, receiver_name, tax_rate, inspection_status,
  qualified_qty, rejected_qty, inspector_name, inspected_at, payable_due_date,
  client_request_id, created_by, updated_by
)
VALUES
('da000000-0000-4000-8000-000000000001', 'default', 'DEMO-SHD-001',
 (SELECT id FROM procurement_purchase_orders WHERE tenant_id = 'default' AND code = 'DEMO-CGD-001'),
 (SELECT id FROM inventory_parts WHERE tenant_id = 'default' AND code = 'DEMO-WL-001'),
 1.00, 520000.00, 520000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 3, 5),
 'DEMO-DELIVERY-001', '仓储物流流程测试员', 13.00, 'PASSED', 1.00, 0,
 '质量管理流程测试员', CURRENT_TIMESTAMP,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 5, 5), 'demo-receipt-001', 'demo-seed', 'demo-seed'),
('da000000-0000-4000-8000-000000000002', 'default', 'DEMO-SHD-002',
 (SELECT id FROM procurement_purchase_orders WHERE tenant_id = 'default' AND code = 'DEMO-CGD-002'),
 (SELECT id FROM inventory_parts WHERE tenant_id = 'default' AND code = 'DEMO-WL-001'),
 1.00, 310000.00, 310000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 3, 20),
 'DEMO-DELIVERY-002', '仓储物流流程测试员', 13.00, 'PASSED', 1.00, 0,
 '质量管理流程测试员', CURRENT_TIMESTAMP,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 5, 20), 'demo-receipt-002', 'demo-seed', 'demo-seed'),
('da000000-0000-4000-8000-000000000003', 'default', 'DEMO-SHD-003',
 (SELECT id FROM procurement_purchase_orders WHERE tenant_id = 'default' AND code = 'DEMO-CGD-003'),
 (SELECT id FROM inventory_parts WHERE tenant_id = 'default' AND code = 'DEMO-WL-001'),
 1.00, 820000.00, 820000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 8),
 'DEMO-DELIVERY-003', '仓储物流流程测试员', 13.00, 'PASSED', 1.00, 0,
 '质量管理流程测试员', CURRENT_TIMESTAMP,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 4, 8), 'demo-receipt-003', 'demo-seed', 'demo-seed'),
('da000000-0000-4000-8000-000000000004', 'default', 'DEMO-SHD-004',
 (SELECT id FROM procurement_purchase_orders WHERE tenant_id = 'default' AND code = 'DEMO-CGD-004'),
 (SELECT id FROM inventory_parts WHERE tenant_id = 'default' AND code = 'DEMO-WL-001'),
 1.00, 210000.00, 210000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 20),
 'DEMO-DELIVERY-004', '仓储物流流程测试员', 13.00, 'PASSED', 1.00, 0,
 '质量管理流程测试员', CURRENT_TIMESTAMP,
 make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 4, 20), 'demo-receipt-004', 'demo-seed', 'demo-seed')
ON CONFLICT (tenant_id, code) DO UPDATE SET
  order_id = EXCLUDED.order_id,
  part_id = EXCLUDED.part_id,
  quantity = EXCLUDED.quantity,
  unit_price = EXCLUDED.unit_price,
  amount = EXCLUDED.amount,
  received_date = EXCLUDED.received_date,
  tax_rate = EXCLUDED.tax_rate,
  inspection_status = EXCLUDED.inspection_status,
  qualified_qty = EXCLUDED.qualified_qty,
  rejected_qty = EXCLUDED.rejected_qty,
  payable_due_date = EXCLUDED.payable_due_date,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO fin_procurement_payables (
  id, tenant_id, code, supplier_id, order_id, receipt_id, organization_id,
  amount, tax_rate, paid_amount, due_date, status, created_by, updated_by
)
VALUES
('db000000-0000-4000-8000-000000000001', 'default', 'DEMO-YF-001',
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 (SELECT id FROM procurement_purchase_orders WHERE tenant_id = 'default' AND code = 'DEMO-CGD-001'),
 (SELECT id FROM procurement_goods_receipts WHERE tenant_id = 'default' AND code = 'DEMO-SHD-001'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKET_SALES_CENTER'),
 520000.00, 13.00, 420000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 9, 30),
 'PARTIAL_PAID', 'demo-seed', 'demo-seed'),
('db000000-0000-4000-8000-000000000002', 'default', 'DEMO-YF-002',
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 (SELECT id FROM procurement_purchase_orders WHERE tenant_id = 'default' AND code = 'DEMO-CGD-002'),
 (SELECT id FROM procurement_goods_receipts WHERE tenant_id = 'default' AND code = 'DEMO-SHD-002'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKET_SALES_CENTER'),
 310000.00, 13.00, 180000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 8, 31),
 'PARTIAL_PAID', 'demo-seed', 'demo-seed'),
('db000000-0000-4000-8000-000000000003', 'default', 'DEMO-YF-003',
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 (SELECT id FROM procurement_purchase_orders WHERE tenant_id = 'default' AND code = 'DEMO-CGD-003'),
 (SELECT id FROM procurement_goods_receipts WHERE tenant_id = 'default' AND code = 'DEMO-SHD-003'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKETING_DEPARTMENT'),
 820000.00, 13.00, 640000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 10, 31),
 'PARTIAL_PAID', 'demo-seed', 'demo-seed'),
('db000000-0000-4000-8000-000000000004', 'default', 'DEMO-YF-004',
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 (SELECT id FROM procurement_purchase_orders WHERE tenant_id = 'default' AND code = 'DEMO-CGD-004'),
 (SELECT id FROM procurement_goods_receipts WHERE tenant_id = 'default' AND code = 'DEMO-SHD-004'),
 (SELECT id FROM sys_organizations WHERE tenant_id = 'default' AND code = 'MARKETING_DEPARTMENT'),
 210000.00, 13.00, 210000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 4, 20),
 'PAID', 'demo-seed', 'demo-seed')
ON CONFLICT (tenant_id, code) DO UPDATE SET
  supplier_id = EXCLUDED.supplier_id,
  order_id = EXCLUDED.order_id,
  receipt_id = EXCLUDED.receipt_id,
  organization_id = EXCLUDED.organization_id,
  amount = EXCLUDED.amount,
  tax_rate = EXCLUDED.tax_rate,
  paid_amount = EXCLUDED.paid_amount,
  due_date = EXCLUDED.due_date,
  status = EXCLUDED.status,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO fin_payment_applications (
  id, tenant_id, code, payable_id, supplier_id, requested_amount, requested_date,
  applicant_name, purpose, status, approval_comment, approver_name, approved_at,
  payment_id, created_by, updated_by
)
VALUES
('dc000000-0000-4000-8000-000000000001', 'default', 'DEMO-FKSQ-001',
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-001'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 220000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 3, 1), '采购部流程测试员',
 '联城项目首笔设备付款', 'PAID', '演示付款审批通过', '财务负责人流程测试员', CURRENT_TIMESTAMP,
 'dd000000-0000-4000-8000-000000000001', 'demo-seed', 'demo-seed'),
('dc000000-0000-4000-8000-000000000002', 'default', 'DEMO-FKSQ-002',
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-002'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 180000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 6, 20), '采购部流程测试员',
 '澄江项目设备付款', 'PAID', '演示付款审批通过', '财务负责人流程测试员', CURRENT_TIMESTAMP,
 'dd000000-0000-4000-8000-000000000003', 'demo-seed', 'demo-seed'),
('dc000000-0000-4000-8000-000000000003', 'default', 'DEMO-FKSQ-003',
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-003'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 300000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 8), '采购部流程测试员',
 '海岳项目首笔设备付款', 'PAID', '演示付款审批通过', '财务负责人流程测试员', CURRENT_TIMESTAMP,
 'dd000000-0000-4000-8000-000000000004', 'demo-seed', 'demo-seed'),
('dc000000-0000-4000-8000-000000000004', 'default', 'DEMO-FKSQ-004',
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-004'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 210000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 4, 10), '采购部流程测试员',
 '城南医疗项目设备付款', 'PAID', '演示付款审批通过', '财务负责人流程测试员', CURRENT_TIMESTAMP,
 'dd000000-0000-4000-8000-000000000006', 'demo-seed', 'demo-seed'),
('dc000000-0000-4000-8000-000000000005', 'default', 'DEMO-FKSQ-001-2',
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-001'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 200000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 5, 5), '采购部流程测试员',
 '联城项目第二笔设备付款', 'PAID', '演示付款审批通过', '财务负责人流程测试员', CURRENT_TIMESTAMP,
 'dd000000-0000-4000-8000-000000000002', 'demo-seed', 'demo-seed'),
('dc000000-0000-4000-8000-000000000006', 'default', 'DEMO-FKSQ-003-2',
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-003'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 340000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 6, 15), '采购部流程测试员',
 '海岳项目第二笔设备付款', 'PAID', '演示付款审批通过', '财务负责人流程测试员', CURRENT_TIMESTAMP,
 'dd000000-0000-4000-8000-000000000005', 'demo-seed', 'demo-seed')
ON CONFLICT (tenant_id, code) DO UPDATE SET
  payable_id = EXCLUDED.payable_id,
  supplier_id = EXCLUDED.supplier_id,
  requested_amount = EXCLUDED.requested_amount,
  requested_date = EXCLUDED.requested_date,
  applicant_name = EXCLUDED.applicant_name,
  purpose = EXCLUDED.purpose,
  status = EXCLUDED.status,
  approval_comment = EXCLUDED.approval_comment,
  approver_name = EXCLUDED.approver_name,
  approved_at = EXCLUDED.approved_at,
  payment_id = EXCLUDED.payment_id,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO fin_payment_records (
  id, tenant_id, code, application_id, payable_id, supplier_id, amount,
  paid_date, payment_method, bank_reference, payer_name, created_by, updated_by
)
VALUES
('dd000000-0000-4000-8000-000000000001', 'default', 'DEMO-FK-001-1',
 (SELECT id FROM fin_payment_applications WHERE tenant_id = 'default' AND code = 'DEMO-FKSQ-001'),
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-001'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 220000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 3, 5), 'BANK_TRANSFER',
 'DEMO-BANK-001-1', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('dd000000-0000-4000-8000-000000000002', 'default', 'DEMO-FK-001-2',
 (SELECT id FROM fin_payment_applications WHERE tenant_id = 'default' AND code = 'DEMO-FKSQ-001-2'),
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-001'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 200000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 5, 8), 'BANK_TRANSFER',
 'DEMO-BANK-001-2', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('dd000000-0000-4000-8000-000000000003', 'default', 'DEMO-FK-002-1',
 (SELECT id FROM fin_payment_applications WHERE tenant_id = 'default' AND code = 'DEMO-FKSQ-002'),
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-002'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 180000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 6, 25), 'BANK_TRANSFER',
 'DEMO-BANK-002-1', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('dd000000-0000-4000-8000-000000000004', 'default', 'DEMO-FK-003-1',
 (SELECT id FROM fin_payment_applications WHERE tenant_id = 'default' AND code = 'DEMO-FKSQ-003'),
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-003'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 300000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2, 12), 'BANK_TRANSFER',
 'DEMO-BANK-003-1', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('dd000000-0000-4000-8000-000000000005', 'default', 'DEMO-FK-003-2',
 (SELECT id FROM fin_payment_applications WHERE tenant_id = 'default' AND code = 'DEMO-FKSQ-003-2'),
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-003'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 340000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 6, 20), 'BANK_TRANSFER',
 'DEMO-BANK-003-2', '出纳流程测试员', 'demo-seed', 'demo-seed'),
('dd000000-0000-4000-8000-000000000006', 'default', 'DEMO-FK-004-1',
 (SELECT id FROM fin_payment_applications WHERE tenant_id = 'default' AND code = 'DEMO-FKSQ-004'),
 (SELECT id FROM fin_procurement_payables WHERE tenant_id = 'default' AND code = 'DEMO-YF-004'),
 (SELECT id FROM procurement_suppliers WHERE tenant_id = 'default' AND code = 'DEMO-GYS-001'),
 210000.00, make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 4, 15), 'BANK_TRANSFER',
 'DEMO-BANK-004-1', '出纳流程测试员', 'demo-seed', 'demo-seed')
ON CONFLICT (tenant_id, code) DO UPDATE SET
  application_id = EXCLUDED.application_id,
  payable_id = EXCLUDED.payable_id,
  supplier_id = EXCLUDED.supplier_id,
  amount = EXCLUDED.amount,
  paid_date = EXCLUDED.paid_date,
  payment_method = EXCLUDED.payment_method,
  bank_reference = EXCLUDED.bank_reference,
  payer_name = EXCLUDED.payer_name,
  updated_at = CURRENT_TIMESTAMP;

COMMIT;

SELECT 'demo customers' AS item, COUNT(*) AS count
FROM crm_customers WHERE tenant_id = 'default' AND code LIKE 'DEMO-KH-%'
UNION ALL
SELECT 'demo projects', COUNT(*)
FROM project_projects WHERE tenant_id = 'default' AND code LIKE 'DEMO-XM-%'
UNION ALL
SELECT 'demo receipts', COUNT(*)
FROM fin_receivable_receipts WHERE tenant_id = 'default' AND reference_no LIKE 'DEMO-HK-%'
UNION ALL
SELECT 'demo payments', COUNT(*)
FROM fin_payment_records WHERE tenant_id = 'default' AND code LIKE 'DEMO-FK-%';
