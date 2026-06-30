insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000011', 'default', 'EXECUTIVE_OFFICE', '总经办', 'DEPARTMENT', 10,
       (select id from sys_organizations where tenant_id = 'default' and code = 'ROOT'), true,
       '战略决策支持、经营协调、重大事项督办与管理层服务。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'EXECUTIVE_OFFICE');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000012', 'default', 'MARKET_SALES_CENTER', '市场销售中心', 'DEPARTMENT', 20,
       (select id from sys_organizations where tenant_id = 'default' and code = 'ROOT'), true,
       '市场拓展、客户开发、商务管理与收入目标达成。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'MARKET_SALES_CENTER');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000013', 'default', 'PROJECT_DELIVERY_CENTER', '项目交付中心', 'DEPARTMENT', 30,
       (select id from sys_organizations where tenant_id = 'default' and code = 'ROOT'), true,
       '项目计划、技术实施、交付验收和质量管理。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'PROJECT_DELIVERY_CENTER');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000014', 'default', 'SUPPLY_CHAIN_CENTER', '供应链中心', 'DEPARTMENT', 40,
       (select id from sys_organizations where tenant_id = 'default' and code = 'ROOT'), true,
       '供应商、采购、仓储物流与物料成本协同。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'SUPPLY_CHAIN_CENTER');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000015', 'default', 'FINANCE_DEPARTMENT', '财务部', 'DEPARTMENT', 50,
       (select id from sys_organizations where tenant_id = 'default' and code = 'ROOT'), true,
       '预算、应收应付、资金、税务和经营核算。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'FINANCE_DEPARTMENT');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000016', 'default', 'GENERAL_MANAGEMENT', '综合管理部', 'DEPARTMENT', 60,
       (select id from sys_organizations where tenant_id = 'default' and code = 'ROOT'), true,
       '人力、行政、制度、档案和后勤保障。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'GENERAL_MANAGEMENT');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000021', 'default', 'MARKETING_DEPARTMENT', '市场部', 'DEPARTMENT', 10,
       (select id from sys_organizations where tenant_id = 'default' and code = 'MARKET_SALES_CENTER'), true,
       '品牌建设、渠道拓展、市场调研和推广活动。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'MARKETING_DEPARTMENT');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000023', 'default', 'CUSTOMER_SERVICE_DEPARTMENT', '客户服务部', 'DEPARTMENT', 30,
       (select id from sys_organizations where tenant_id = 'default' and code = 'MARKET_SALES_CENTER'), true,
       '客户接洽、服务协调、回访投诉和满意度管理。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'CUSTOMER_SERVICE_DEPARTMENT');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000031', 'default', 'PROJECT_MANAGEMENT_DEPARTMENT', '项目管理部', 'DEPARTMENT', 10,
       (select id from sys_organizations where tenant_id = 'default' and code = 'PROJECT_DELIVERY_CENTER'), true,
       '项目立项、计划进度、成本风险和验收归档。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'PROJECT_MANAGEMENT_DEPARTMENT');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000032', 'default', 'TECHNICAL_SERVICE_DEPARTMENT', '技术服务部', 'DEPARTMENT', 20,
       (select id from sys_organizations where tenant_id = 'default' and code = 'PROJECT_DELIVERY_CENTER'), true,
       '技术方案、实施支持、现场服务和问题处理。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'TECHNICAL_SERVICE_DEPARTMENT');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000033', 'default', 'QUALITY_DEPARTMENT', '质量管理部', 'DEPARTMENT', 30,
       (select id from sys_organizations where tenant_id = 'default' and code = 'PROJECT_DELIVERY_CENTER'), true,
       '质量标准、过程检查、交付评审和持续改进。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'QUALITY_DEPARTMENT');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000041', 'default', 'PROCUREMENT_DEPARTMENT', '采购部', 'DEPARTMENT', 10,
       (select id from sys_organizations where tenant_id = 'default' and code = 'SUPPLY_CHAIN_CENTER'), true,
       '采购申请、询比价、订单执行和供应商管理。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'PROCUREMENT_DEPARTMENT');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000042', 'default', 'WAREHOUSE_LOGISTICS_DEPARTMENT', '仓储物流部', 'DEPARTMENT', 20,
       (select id from sys_organizations where tenant_id = 'default' and code = 'SUPPLY_CHAIN_CENTER'), true,
       '入出库、盘点、库位、安全库存和物流协调。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'WAREHOUSE_LOGISTICS_DEPARTMENT');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000061', 'default', 'HUMAN_RESOURCES_DEPARTMENT', '人力资源部', 'DEPARTMENT', 10,
       (select id from sys_organizations where tenant_id = 'default' and code = 'GENERAL_MANAGEMENT'), true,
       '招聘、员工档案、合同、考勤、薪酬绩效和培训。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'HUMAN_RESOURCES_DEPARTMENT');

insert into sys_organizations (
  id, tenant_id, code, name, type, sort_order, parent_id, enabled, description, created_at, updated_at
)
select '00000000-0000-4000-8000-000000000062', 'default', 'ADMINISTRATION_DEPARTMENT', '行政部', 'DEPARTMENT', 20,
       (select id from sys_organizations where tenant_id = 'default' and code = 'GENERAL_MANAGEMENT'), true,
       '行政事务、资产车辆、用章、会议和办公保障。', current_timestamp, current_timestamp
where not exists (select 1 from sys_organizations where tenant_id = 'default' and code = 'ADMINISTRATION_DEPARTMENT');

update sys_organizations
set type = 'COMPANY', sort_order = 0, parent_id = null, enabled = true,
    description = '公司经营管理总部，统筹战略、经营目标和资源配置。', updated_at = current_timestamp
where tenant_id = 'default' and code = 'ROOT';

update sys_organizations
set name = '销售部', type = 'DEPARTMENT', sort_order = 20,
    parent_id = (select id from sys_organizations where tenant_id = 'default' and code = 'MARKET_SALES_CENTER'),
    enabled = true, description = '客户开发、商机推进、报价谈判、合同签订与回款协同。',
    updated_at = current_timestamp
where tenant_id = 'default' and code = 'DEPT_SALES';
