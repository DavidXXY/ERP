create table if not exists crm_follow_ups (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  customer_id uuid not null references crm_customers(id),
  opportunity_id uuid references crm_opportunities(id),
  type varchar(32) not null,
  subject varchar(160) not null,
  content varchar(1200) not null,
  followed_at timestamp not null,
  next_action varchar(240),
  owner_name varchar(80) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64)
);

create index if not exists idx_crm_opportunity_customer on crm_opportunities (customer_id);
create index if not exists idx_crm_quote_opportunity on crm_quote_plans (opportunity_id);
create index if not exists idx_crm_follow_up_customer on crm_follow_ups (customer_id, followed_at);

merge into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) key (tenant_id, code) values
('00000000-0000-4000-8000-000000002301', 'default', 'crm:opportunity:create', '商机新增', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002302', 'default', 'crm:opportunity:update', '商机推进', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002303', 'default', 'crm:quote:view', '报价方案查看', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002304', 'default', 'crm:quote:create', '报价方案新增', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002305', 'default', 'crm:quote:submit', '报价提交审批', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002306', 'default', 'crm:followup:view', '跟进回访查看', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002307', 'default', 'crm:followup:create', '跟进回访新增', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002308', 'default', 'crm:renewal:view', '续约管理查看', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002309', 'default', 'crm:receivable:view', '合同应收查看', 'crm', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002310', 'default', 'crm:profile:view', '客户经营画像查看', 'crm', current_timestamp, current_timestamp);

insert into sys_role_permissions (role_id, permission_id)
select r.id, p.id
from sys_roles r
join sys_permissions p on p.tenant_id = r.tenant_id
where r.tenant_id = 'default'
  and r.code in ('ADMIN', 'CRM_MANAGER')
  and p.code like 'crm:%'
  and not exists (
    select 1 from sys_role_permissions rp
    where rp.role_id = r.id and rp.permission_id = p.id
  );

merge into crm_opportunities (
  id, tenant_id, customer_id, code, source, need_summary, stage, expected_amount,
  probability, next_action, next_action_at, owner_name, created_at, updated_at
) key (tenant_id, code) values
(
  'dddddddd-1111-4111-8111-dddddddddddd', 'default',
  '11111111-1111-4111-8111-111111111111', 'SJ-2026-021', '老客户追加',
  '虹桥车辆基地高压柜改造后的年度维保与红外测温服务', 'SOLUTION', 680000,
  60, '确认设备清单和夜间作业窗口', '2026-07-01', '客户经理A', current_timestamp, current_timestamp
),
(
  'eeeeeeee-2222-4222-8222-eeeeeeeeeeee', 'default',
  '22222222-2222-4222-8222-222222222222', 'SJ-2026-028', '客户转介绍',
  '新增两座泵站的驻场运维与年度检测服务', 'QUOTATION', 960000,
  75, '提交分项报价并确认人员配置', '2026-07-03', '客户经理B', current_timestamp, current_timestamp
);

merge into crm_quote_plans (
  id, tenant_id, customer_id, opportunity_id, code, service_scope, inspect_cycle,
  payment_nodes, amount, status, created_at, updated_at
) key (tenant_id, code) values (
  'ffffffff-1111-4111-8111-ffffffffffff', 'default',
  '11111111-1111-4111-8111-111111111111', 'dddddddd-1111-4111-8111-dddddddddddd',
  'BJ-2026-035', '高压柜季度巡检、年度预防性试验、红外测温及7x24故障响应',
  '季度巡检，年度检测', '合同签订30%，半年节点30%，年度验收40%', 680000,
  'DRAFT', current_timestamp, current_timestamp
);

merge into crm_follow_ups (
  id, tenant_id, customer_id, opportunity_id, type, subject, content, followed_at,
  next_action, owner_name, created_at, updated_at
) key (id) values (
  'abababab-1111-4111-8111-abababababab', 'default',
  '11111111-1111-4111-8111-111111111111', 'dddddddd-1111-4111-8111-dddddddddddd',
  'VISIT', '现场设备清单确认', '与设备处核对了68台高压柜和夜间停电窗口，客户要求报价拆分检测与驻场服务。',
  '2026-06-26 14:30:00', '7月1日前提交拆分报价', '客户经理A', current_timestamp, current_timestamp
);
