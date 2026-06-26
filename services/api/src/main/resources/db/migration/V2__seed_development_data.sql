insert into crm_customers (
  id, tenant_id, code, name, industry, level, owner_name, payment_habit,
  risk_status, risk_note, invoice_title, tax_no, bank_name, bank_account,
  registered_address, registered_phone, created_at, updated_at
) values
(
  '11111111-1111-4111-8111-111111111111', 'default', 'KH-001', '华东轨交能源中心',
  '轨道交通', 'STRATEGIC', '客户经理A', '季度付款，信用良好', 'NORMAL',
  '付款节奏稳定，设备扩容改造有追加机会。', '华东轨交能源中心有限公司',
  '91310000MA1GD8A18X', '中国建设银行上海张江支行', '31001588882050011234',
  '上海市浦东新区龙东大道1688号', '021-5899 1026', now(), now()
),
(
  '22222222-2222-4222-8222-222222222222', 'default', 'KH-002', '新澄水务集团',
  '市政水务', 'KEY', '客户经理B', '月结，存在逾期', 'OVERDUE',
  '夜间响应要求提高，本月驻场排班与回款需要同步跟进。', '新澄水务集团有限公司',
  '91320582MA25P6XQ9B', '中国农业银行新澄支行', '10288101040021876',
  '江苏省新澄市水务路66号', '0512-6688 9021', now(), now()
),
(
  '33333333-3333-4333-8333-333333333333', 'default', 'KH-003', '启明商业广场',
  '商业综合体', 'KEY', '客户经理C', '验收后付款', 'RENEWAL_RISK',
  '合同8月到期，客户关注消防联动误报率与续约价格。', '上海启明商业管理有限公司',
  '91310106MA1FY2M33H', '招商银行上海静安支行', '121908774610808',
  '上海市静安区启明路299号', '021-6266 3188', now(), now()
);

insert into crm_customer_contacts (
  tenant_id, customer_id, name, title, phone, email, primary_contact, created_at, updated_at
) values
('default', '11111111-1111-4111-8111-111111111111', '王主任', '设备处', '13800001001', 'wang@example.com', true, now(), now()),
('default', '22222222-2222-4222-8222-222222222222', '刘工', '运维部', '13800002002', 'liu@example.com', true, now(), now()),
('default', '33333333-3333-4333-8333-333333333333', '赵经理', '物业工程', '13800003003', 'zhao@example.com', true, now(), now());

insert into crm_customer_sites (
  tenant_id, customer_id, name, address, created_at, updated_at
) values
('default', '11111111-1111-4111-8111-111111111111', '能源中心配电室', '浦东能源中心配电室', now(), now()),
('default', '11111111-1111-4111-8111-111111111111', '虹桥车辆基地', '虹桥车辆基地', now(), now()),
('default', '22222222-2222-4222-8222-222222222222', '城北泵站', '新澄市城北泵站3号机房', now(), now()),
('default', '33333333-3333-4333-8333-333333333333', 'B1消控中心', '启明商业广场B1消控中心', now(), now());

insert into crm_service_contracts (
  id, tenant_id, customer_id, code, project_name, contract_type, amount,
  start_date, end_date, service_cycle, status, created_at, updated_at
) values
(
  'aaaaaaaa-1111-4111-8111-aaaaaaaaaaaa', 'default', '11111111-1111-4111-8111-111111111111',
  'HT-2026-018', '年度高压配电维保', '年度包年维保', 1260000,
  '2026-01-01', '2026-12-31', '季度巡检', 'ACTIVE', now(), now()
),
(
  'bbbbbbbb-2222-4222-8222-bbbbbbbbbbbb', 'default', '22222222-2222-4222-8222-222222222222',
  'HT-2026-027', '泵站驻场运维', '驻场运维', 880000,
  '2026-03-01', '2027-02-28', '每日巡查', 'OVERDUE_RISK', now(), now()
),
(
  'cccccccc-3333-4333-8333-cccccccccccc', 'default', '33333333-3333-4333-8333-333333333333',
  'HT-2025-144', '消防联动系统维保', '设备质保合同', 420000,
  '2025-08-01', '2026-08-31', '月度检测', 'RENEWAL_PENDING', now(), now()
);

insert into fin_receivables (
  tenant_id, customer_id, contract_id, code, source_no, amount, due_date, status, created_at, updated_at
) values
('default', '11111111-1111-4111-8111-111111111111', 'aaaaaaaa-1111-4111-8111-aaaaaaaaaaaa', 'YS-202606-036', 'HT-2026-018', 315000, '2026-07-10', 'PAYMENT_PENDING', now(), now()),
('default', '22222222-2222-4222-8222-222222222222', 'bbbbbbbb-2222-4222-8222-bbbbbbbbbbbb', 'YS-202606-041', 'HT-2026-027', 146000, '2026-06-30', 'INVOICE_PENDING', now(), now()),
('default', '33333333-3333-4333-8333-333333333333', 'cccccccc-3333-4333-8333-cccccccccccc', 'YS-202605-019', 'HT-2025-144', 8600, '2026-06-20', 'OVERDUE', now(), now());

