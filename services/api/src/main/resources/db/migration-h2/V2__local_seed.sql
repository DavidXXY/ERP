insert into sys_organizations (
  id, tenant_id, code, name, type, created_at, updated_at
) values (
  '00000000-0000-4000-8000-000000000001', 'default', 'ROOT', '工程运维公司', 'COMPANY', current_timestamp, current_timestamp
);

insert into sys_roles (
  id, tenant_id, code, name, data_scope, created_at, updated_at
) values
('00000000-0000-4000-8000-000000000101', 'default', 'ADMIN', '系统管理员', 'ALL', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000000102', 'default', 'CRM_MANAGER', '客户经理', 'DEPARTMENT', current_timestamp, current_timestamp);

insert into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) values
('00000000-0000-4000-8000-000000001001', 'default', 'dashboard:view', '经营驾驶舱查看', 'dashboard', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001101', 'default', 'crm:customer:view', '客户池查看', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001102', 'default', 'crm:customer:create', '客户新增', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001201', 'default', 'crm:opportunity:view', '线索商机查看', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001301', 'default', 'crm:contract:view', '维保合同查看', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001401', 'default', 'procurement:view', '供应链采购查看', 'procurement', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001402', 'default', 'procurement:supplier:create', '供应商新增', 'procurement', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001403', 'default', 'procurement:purchase:create', '采购单据新增', 'procurement', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001501', 'default', 'project:view', '项目管理查看', 'project', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001502', 'default', 'project:create', '项目新增', 'project', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001601', 'default', 'inventory:view', '备件仓储查看', 'inventory', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001602', 'default', 'inventory:part:create', '备件新增', 'inventory', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001603', 'default', 'inventory:movement:create', '库存流水新增', 'inventory', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001701', 'default', 'finance:view', '财务资金查看', 'finance', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001801', 'default', 'system:view', '系统设置查看', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001901', 'default', 'system:user:view', '员工管理查看', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001902', 'default', 'system:user:create', '员工新增', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001903', 'default', 'system:user:update', '员工编辑', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001904', 'default', 'system:user:delete', '员工删除', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000001905', 'default', 'system:user:reset-password', '员工密码重置', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002001', 'default', 'system:role:view', '角色管理查看', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002002', 'default', 'system:role:create', '角色新增', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002003', 'default', 'system:role:update', '角色编辑', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002004', 'default', 'system:role:delete', '角色删除', 'system', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002101', 'default', 'system:permission:view', '权限管理查看', 'system', current_timestamp, current_timestamp);

insert into sys_users (
  id, tenant_id, org_id, username, display_name, password_hash, phone, email, enabled, created_at, updated_at
) values (
  '00000000-0000-4000-8000-000000000201',
  'default',
  '00000000-0000-4000-8000-000000000001',
  'admin',
  '系统管理员',
  '{noop}Admin@123',
  '13800000000',
  'admin@example.com',
  true,
  current_timestamp,
  current_timestamp
);

insert into sys_user_roles (user_id, role_id) values
('00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000101');

insert into sys_role_permissions (role_id, permission_id)
select '00000000-0000-4000-8000-000000000101', id
from sys_permissions
where tenant_id = 'default';

insert into crm_customers (
  id, tenant_id, code, name, industry, level, owner_name, payment_habit,
  risk_status, risk_note, invoice_title, tax_no, bank_name, bank_account,
  registered_address, registered_phone, created_at, updated_at
) values
(
  '11111111-1111-4111-8111-111111111111', 'default', 'KH-20260703-0001', '华东轨交能源中心',
  '轨道交通', 'STRATEGIC', '客户经理A', '季度付款，信用良好', 'NORMAL',
  '付款节奏稳定，设备扩容改造有追加机会。', '华东轨交能源中心有限公司',
  '91310000MA1GD8A18X', '中国建设银行上海张江支行', '31001588882050011234',
  '上海市浦东新区龙东大道1688号', '021-5899 1026', current_timestamp, current_timestamp
),
(
  '22222222-2222-4222-8222-222222222222', 'default', 'KH-20260703-0002', '新澄水务集团',
  '市政水务', 'KEY', '客户经理B', '月结，存在逾期', 'OVERDUE',
  '夜间响应要求提高，本月驻场排班与回款需要同步跟进。', '新澄水务集团有限公司',
  '91320582MA25P6XQ9B', '中国农业银行新澄支行', '10288101040021876',
  '江苏省新澄市水务路66号', '0512-6688 9021', current_timestamp, current_timestamp
);

insert into crm_customer_contacts (
  tenant_id, customer_id, name, title, phone, email, primary_contact, created_at, updated_at
) values
('default', '11111111-1111-4111-8111-111111111111', '王主任', '设备处', '13800001001', 'wang@example.com', true, current_timestamp, current_timestamp),
('default', '22222222-2222-4222-8222-222222222222', '刘工', '运维部', '13800002002', 'liu@example.com', true, current_timestamp, current_timestamp);

insert into crm_customer_sites (
  tenant_id, customer_id, name, address, created_at, updated_at
) values
('default', '11111111-1111-4111-8111-111111111111', '能源中心配电室', '浦东能源中心配电室', current_timestamp, current_timestamp),
('default', '22222222-2222-4222-8222-222222222222', '城北泵站', '新澄市城北泵站3号机房', current_timestamp, current_timestamp);

insert into crm_service_contracts (
  id, tenant_id, customer_id, code, project_name, contract_type, amount,
  start_date, end_date, service_cycle, status, created_at, updated_at
) values
(
  'aaaaaaaa-1111-4111-8111-aaaaaaaaaaaa', 'default', '11111111-1111-4111-8111-111111111111',
  'HT-2026-018', '年度高压配电维保', '年度包年维保', 1260000,
  '2026-01-01', '2026-12-31', '季度巡检', 'ACTIVE', current_timestamp, current_timestamp
);

insert into fin_receivables (
  tenant_id, customer_id, contract_id, code, source_no, amount, due_date, status, created_at, updated_at
) values
('default', '11111111-1111-4111-8111-111111111111', 'aaaaaaaa-1111-4111-8111-aaaaaaaaaaaa', 'YS-202606-036', 'HT-2026-018', 315000, '2026-07-10', 'PAYMENT_PENDING', current_timestamp, current_timestamp);

insert into project_projects (
  id, tenant_id, customer_id, code, name, stage, budget_amount, actual_cost,
  progress, warranty_end_date, created_at, updated_at
) values
(
  '44444444-1111-4111-8111-444444444444', 'default',
  '11111111-1111-4111-8111-111111111111', 'XM-2026-006',
  '虹桥车辆基地高压柜改造', 'CONSTRUCTION', 1680000, 742000, 45,
  '2027-06-30', current_timestamp, current_timestamp
);

insert into inventory_parts (
  id, tenant_id, code, name, model, stock_qty, safety_qty, location, unit_cost,
  created_at, updated_at
) values
(
  '55555555-1111-4111-8111-555555555555', 'default',
  'BJ-DL-001', '塑壳断路器', 'MCCB-250A', 12, 8, 'A-01-03', 680,
  current_timestamp, current_timestamp
),
(
  '55555555-2222-4222-8222-555555555555', 'default',
  'BJ-SB-014', '压力传感器', 'PT-0.6MPa', 3, 5, 'B-02-01', 430,
  current_timestamp, current_timestamp
);

insert into inventory_stock_movements (
  tenant_id, part_id, movement_type, quantity, source_no, remark, created_at, updated_at
) values
('default', '55555555-1111-4111-8111-555555555555', 'INBOUND', 12, 'INIT-STOCK', '期初库存', current_timestamp, current_timestamp),
('default', '55555555-2222-4222-8222-555555555555', 'INBOUND', 3, 'INIT-STOCK', '期初库存，低于安全库存', current_timestamp, current_timestamp);

insert into procurement_suppliers (
  id, tenant_id, code, name, category, contact_name, phone, settlement_terms,
  risk_status, created_at, updated_at
) values
(
  '66666666-1111-4111-8111-666666666666', 'default',
  'GYS-001', '上海明启电气设备有限公司', '电气备件', '陈经理',
  '13900001111', '月结30天', 'NORMAL', current_timestamp, current_timestamp
);

insert into procurement_purchase_requests (
  id, tenant_id, code, requester_name, part_id, part_name, quantity, expected_date,
  reason, status, approval_status, created_at, updated_at
) values
(
  '77777777-1111-4111-8111-777777777777', 'default',
  'CGSQ-202606-001', '仓库管理员', '55555555-2222-4222-8222-555555555555',
  '压力传感器', 10, '2026-07-05', '低于安全库存，补货用于泵站抢修',
  'SUBMITTED', 'PENDING', current_timestamp, current_timestamp
);
