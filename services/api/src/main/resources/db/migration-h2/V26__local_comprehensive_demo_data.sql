-- Local-only comprehensive demo data. Every primary business list receives at least 20 linked rows.

insert into sys_organizations (code, name, type, sort_order, leader_name, phone, enabled, description)
select 'DEMO-ORG-' || right('000' || cast(r."X" as varchar), 3),
       '测试业务组 ' || r."X", 'DEPARTMENT', 100 + r."X", '测试负责人' || r."X",
       '1380000' || right('0000' || cast(r."X" as varchar), 4), true, '本地演示组织'
from system_range(1, 20) r;

insert into sys_roles (code, name, data_scope, built_in)
select 'DEMO_ROLE_' || right('000' || cast(r."X" as varchar), 3),
       '测试角色 ' || r."X",
       case mod(r."X", 4) when 0 then 'ALL' when 1 then 'SELF' when 2 then 'DEPT' else 'DEPT_AND_SUB' end,
       false
from system_range(1, 20) r;

insert into sys_users (org_id, username, display_name, password_hash, phone, email, enabled)
select organization.id,
       'demo' || right('000' || cast(r."X" as varchar), 3),
       '测试员工' || r."X",
       administrator.password_hash,
       '1390000' || right('0000' || cast(r."X" as varchar), 4),
       'demo' || r."X" || '@example.com', true
from system_range(1, 20) r
join sys_organizations organization
  on organization.code = 'DEMO-ORG-' || right('000' || cast(r."X" as varchar), 3)
cross join (select password_hash from sys_users where username = 'admin') administrator;

insert into sys_user_roles (user_id, role_id)
select users.id, roles.id
from system_range(1, 20) r
join sys_users users on users.username = 'demo' || right('000' || cast(r."X" as varchar), 3)
join sys_roles roles on roles.code = 'DEMO_ROLE_' || right('000' || cast(r."X" as varchar), 3);

insert into qual_employees (
  external_id, system_user_id, organization_id, name, work_no, department, position_name,
  phone, entry_date, employment_status, contract_start, contract_end, social_security_unit,
  social_security_start, social_security_end, remark
)
select 'DEMO-EMP-' || right('000' || cast(r."X" as varchar), 3), users.id, organization.id,
       users.display_name, 'YG-DEMO-' || right('000' || cast(r."X" as varchar), 3),
       organization.name, case mod(r."X", 4) when 0 then '项目经理' when 1 then '服务工程师' when 2 then '采购专员' else '财务专员' end,
       users.phone, dateadd('MONTH', -r."X", current_date), 'ACTIVE', dateadd('MONTH', -r."X", current_date),
       dateadd('YEAR', 3, current_date), '测试公司', dateadd('MONTH', -r."X", current_date),
       dateadd('YEAR', 1, current_date), '本地演示人员'
from system_range(1, 20) r
join sys_users users on users.username = 'demo' || right('000' || cast(r."X" as varchar), 3)
join sys_organizations organization on organization.id = users.org_id;

insert into hr_employee_contracts (
  employee_id, contract_no, contract_type, sign_date, start_date, end_date,
  probation_end_date, status, attachments_json, remark
)
select employee.id, 'DEMO-LABOR-' || right('000' || cast(r."X" as varchar), 3), '劳动合同',
       dateadd('MONTH', -r."X", current_date), dateadd('MONTH', -r."X", current_date),
       dateadd('YEAR', 3, current_date), dateadd('MONTH', 3 - r."X", current_date),
       'ACTIVE', '[]', '本地演示合同'
from system_range(1, 20) r
join qual_employees employee on employee.external_id = 'DEMO-EMP-' || right('000' || cast(r."X" as varchar), 3);

insert into hr_employee_certificates (
  user_id, certificate_type, certificate_no, issue_date, expiry_date, issuing_authority
)
select users.id, case mod(r."X", 3) when 0 then '高压电工证' when 1 then '低压电工证' else '安全员证' end,
       'DEMO-HRCERT-' || right('000' || cast(r."X" as varchar), 3), dateadd('YEAR', -1, current_date),
       dateadd('YEAR', 2, current_date), '测试发证机构'
from system_range(1, 20) r
join sys_users users on users.username = 'demo' || right('000' || cast(r."X" as varchar), 3);

insert into qual_company_qualifications (
  external_id, subject_company, name, category, level_name, certificate_no, issuer,
  issue_date, valid_from, valid_to, annual_review_date, renewal_date, scope_text,
  project_types_json, holder_branch, storage_location, available_for_tender,
  manual_status, locked, attachments_json, remark
)
select 'DEMO-CQ-' || right('000' || cast(r."X" as varchar), 3), '测试工程有限公司',
       '企业资质证书 ' || r."X", case mod(r."X", 3) when 0 then '施工资质' when 1 then '安全许可' else '体系认证' end,
       '测试等级 ' || mod(r."X", 4), 'DEMO-CQNO-' || right('000' || cast(r."X" as varchar), 3), '测试主管部门',
       dateadd('YEAR', -1, current_date), dateadd('YEAR', -1, current_date), dateadd('YEAR', 2, current_date),
       dateadd('MONTH', 6, current_date), dateadd('YEAR', 1, current_date), '测试资质承揽范围',
       '["新建工程","改造工程"]', '总部', '电子档案柜', true, 'NORMAL', false, '[]', '本地演示资质'
from system_range(1, 20) r;

insert into qual_personnel_certificates (
  external_id, employee_id, name, certificate_type, certificate_no, specialty,
  company_registered, issue_date, valid_to, review_date, available_for_tender,
  manual_status, locked, attachments_json, remark
)
select 'DEMO-PC-' || right('000' || cast(r."X" as varchar), 3), employee.id,
       '人员专业证书 ' || r."X", '职业资格', 'DEMO-PCNO-' || right('000' || cast(r."X" as varchar), 3),
       case mod(r."X", 3) when 0 then '电气' when 1 then '机电' else '安全' end,
       true, dateadd('YEAR', -1, current_date), dateadd('YEAR', 2, current_date),
       dateadd('MONTH', 9, current_date), true, 'NORMAL', false, '[]', '本地演示人员证书'
from system_range(1, 20) r
join qual_employees employee on employee.external_id = 'DEMO-EMP-' || right('000' || cast(r."X" as varchar), 3);

insert into qual_performances (
  external_id, subject_company, name, client_name, contract_no, contract_date,
  contract_amount, project_type, attachments_json, remark
)
select 'DEMO-PERF-' || right('000' || cast(r."X" as varchar), 3), '测试工程有限公司',
       '示范项目业绩 ' || r."X", '演示客户 ' || r."X", 'DEMO-PERF-HT-' || right('000' || cast(r."X" as varchar), 3),
       dateadd('MONTH', -r."X", current_date), cast(100000 + r."X" * 10000 as varchar),
       case mod(r."X", 3) when 0 then '新建工程' when 1 then '改造工程' else '运维改造' end,
       '[]', '本地演示业绩'
from system_range(1, 20) r;

insert into crm_customers (
  code, name, industry, level, owner_name, payment_habit, risk_status, risk_note,
  invoice_title, tax_no, bank_name, bank_account, registered_address, registered_phone
)
select 'DEMO-CUST-' || right('000' || cast(r."X" as varchar), 3), '演示客户 ' || r."X",
       case mod(r."X", 4) when 0 then '制造业' when 1 then '商业地产' when 2 then '公共事业' else '信息技术' end,
       case mod(r."X", 3) when 0 then 'STRATEGIC' when 1 then 'KEY' else 'NORMAL' end,
       '客户经理' || mod(r."X", 5), '月结30天',
       case when mod(r."X", 7) = 0 then 'OVERDUE' when mod(r."X", 5) = 0 then 'RENEWAL_RISK' else 'NORMAL' end,
       case when mod(r."X", 7) = 0 then '存在逾期测试风险' else '经营正常' end,
       '演示客户 ' || r."X", '91310000DEMO' || right('000' || cast(r."X" as varchar), 3),
       '测试银行上海分行', '62220000' || right('0000' || cast(r."X" as varchar), 4),
       '上海市测试路 ' || r."X" || ' 号', '021-6000' || right('0000' || cast(r."X" as varchar), 4)
from system_range(1, 20) r;

insert into crm_customer_contacts (customer_id, name, title, phone, email, primary_contact)
select customer.id, '联系人' || r."X", '设施负责人',
       '1370000' || right('0000' || cast(r."X" as varchar), 4), 'contact' || r."X" || '@example.com', true
from system_range(1, 20) r
join crm_customers customer on customer.code = 'DEMO-CUST-' || right('000' || cast(r."X" as varchar), 3);

insert into crm_customer_sites (customer_id, name, address, longitude, latitude)
select customer.id, '演示项目现场 ' || r."X", '上海市测试路 ' || r."X" || ' 号',
       121.40 + r."X" / 1000.0, 31.20 + r."X" / 1000.0
from system_range(1, 20) r
join crm_customers customer on customer.code = 'DEMO-CUST-' || right('000' || cast(r."X" as varchar), 3);

insert into crm_opportunities (
  customer_id, code, source, need_summary, stage, expected_amount, probability,
  next_action, next_action_at, owner_name
)
select customer.id, 'DEMO-OPP-' || right('000' || cast(r."X" as varchar), 3), '客户转介绍',
       '配电设施服务与改造需求 ' || r."X",
       case mod(r."X", 6) when 0 then 'LEAD' when 1 then 'QUALIFIED' when 2 then 'SOLUTION' when 3 then 'QUOTATION' when 4 then 'NEGOTIATION' else 'WON' end,
       120000 + r."X" * 10000, 20 + mod(r."X", 8) * 10, '安排现场勘察', dateadd('DAY', r."X", current_date), '客户经理' || mod(r."X", 5)
from system_range(1, 20) r
join crm_customers customer on customer.code = 'DEMO-CUST-' || right('000' || cast(r."X" as varchar), 3);

insert into crm_quote_plans (
  customer_id, opportunity_id, code, service_scope, inspect_cycle, payment_nodes,
  amount, status, version_no, customer_decision, customer_comment,
  customer_decision_by, customer_decided_at
)
select customer.id, opportunity.id, 'DEMO-QUOTE-' || right('000' || cast(r."X" as varchar), 3),
       '年度巡检、缺陷处理与应急保障服务', '每季度一次', '签约30%，进场40%，验收30%',
       100000 + r."X" * 10000,
       case mod(r."X", 5) when 0 then 'DRAFT' when 1 then 'PENDING_APPROVAL' when 2 then 'APPROVED' when 3 then 'CUSTOMER_ACCEPTED' else 'CONVERTED' end,
       1, case when mod(r."X", 5) >= 3 then 'ACCEPTED' else null end,
       case when mod(r."X", 5) >= 3 then '客户确认演示数据' else null end,
       case when mod(r."X", 5) >= 3 then '客户联系人' || r."X" else null end,
       case when mod(r."X", 5) >= 3 then current_timestamp else null end
from system_range(1, 20) r
join crm_customers customer on customer.code = 'DEMO-CUST-' || right('000' || cast(r."X" as varchar), 3)
join crm_opportunities opportunity on opportunity.code = 'DEMO-OPP-' || right('000' || cast(r."X" as varchar), 3);

insert into crm_quote_revisions (
  quote_id, version_no, code, service_scope, inspect_cycle, payment_nodes, amount,
  status, revision_note, editor_name, revised_at
)
select quote.id, 1, quote.code, quote.service_scope, quote.inspect_cycle, quote.payment_nodes,
       quote.amount, quote.status, '演示报价初始版本', '报价专员', dateadd('DAY', -r."X", current_timestamp)
from system_range(1, 20) r
join crm_quote_plans quote on quote.code = 'DEMO-QUOTE-' || right('000' || cast(r."X" as varchar), 3);

insert into crm_quote_approval_records (
  quote_id, decision, comment, approver_name, decided_at, quote_version
)
select quote.id, case when mod(r."X", 6) = 0 then 'REJECTED' else 'APPROVED' end,
       '演示报价审批意见', '销售总监', dateadd('DAY', -r."X", current_timestamp), 1
from system_range(1, 20) r
join crm_quote_plans quote on quote.code = 'DEMO-QUOTE-' || right('000' || cast(r."X" as varchar), 3);

insert into crm_service_contracts (
  customer_id, quote_id, code, project_name, contract_type, amount, start_date,
  end_date, service_cycle, status
)
select customer.id, quote.id, 'DEMO-CONTRACT-' || right('000' || cast(r."X" as varchar), 3),
       '演示服务项目 ' || r."X", '年度服务合同', quote.amount,
       dateadd('MONTH', -2, current_date), dateadd('YEAR', 1, current_date), '季度巡检',
       case when mod(r."X", 6) = 0 then 'RENEWAL_PENDING' else 'ACTIVE' end
from system_range(1, 20) r
join crm_customers customer on customer.code = 'DEMO-CUST-' || right('000' || cast(r."X" as varchar), 3)
join crm_quote_plans quote on quote.code = 'DEMO-QUOTE-' || right('000' || cast(r."X" as varchar), 3);

update crm_quote_approval_records approval
set generated_contract_id = (
  select contract.id from crm_service_contracts contract where contract.quote_id = approval.quote_id
)
where approval.quote_id in (select id from crm_quote_plans where code like 'DEMO-QUOTE-%');

insert into crm_follow_ups (
  customer_id, opportunity_id, type, subject, content, followed_at, next_action, owner_name
)
select customer.id, opportunity.id,
       case mod(r."X", 4) when 0 then 'VISIT' when 1 then 'PHONE' when 2 then 'CALLBACK' else 'COMPLAINT' end,
       '演示客户跟进 ' || r."X", '已沟通服务范围、现场计划和商务条件。',
       dateadd('DAY', -r."X", current_timestamp), '准备下一轮方案沟通', '客户经理' || mod(r."X", 5)
from system_range(1, 20) r
join crm_customers customer on customer.code = 'DEMO-CUST-' || right('000' || cast(r."X" as varchar), 3)
join crm_opportunities opportunity on opportunity.code = 'DEMO-OPP-' || right('000' || cast(r."X" as varchar), 3);

insert into fin_receivables (
  customer_id, contract_id, code, source_no, amount, due_date, status,
  invoice_no, invoice_date, settled_amount
)
select customer.id, contract.id, 'DEMO-AR-' || right('000' || cast(r."X" as varchar), 3), contract.code,
       50000 + r."X" * 5000, dateadd('DAY', 30 - r."X", current_date),
       case mod(r."X", 4) when 0 then 'INVOICE_PENDING' when 1 then 'PAYMENT_PENDING' when 2 then 'OVERDUE' else 'SETTLED' end,
       case when mod(r."X", 4) <> 0 then 'DEMO-INV-' || right('000' || cast(r."X" as varchar), 3) else null end,
       case when mod(r."X", 4) <> 0 then dateadd('DAY', -r."X", current_date) else null end,
       case when mod(r."X", 4) = 3 then 50000 + r."X" * 5000 else 10000 end
from system_range(1, 20) r
join crm_customers customer on customer.code = 'DEMO-CUST-' || right('000' || cast(r."X" as varchar), 3)
join crm_service_contracts contract on contract.code = 'DEMO-CONTRACT-' || right('000' || cast(r."X" as varchar), 3);

insert into fin_receivable_receipts (receivable_id, amount, received_date, reference_no, recorder_name)
select receivable.id, case when receivable.status = 'SETTLED' then receivable.amount else 10000 end,
       dateadd('DAY', -r."X", current_date), 'DEMO-RECEIPT-' || right('000' || cast(r."X" as varchar), 3), '出纳员'
from system_range(1, 20) r
join fin_receivables receivable on receivable.code = 'DEMO-AR-' || right('000' || cast(r."X" as varchar), 3);

insert into project_projects (
  customer_id, contract_id, code, name, stage, project_type, manager_name, site_address,
  budget_amount, actual_cost, progress, contract_amount, planned_start_date,
  planned_end_date, approval_status, approval_comment, approver_name, approved_at,
  warranty_end_date
)
select customer.id, contract.id, 'DEMO-PROJ-' || right('000' || cast(r."X" as varchar), 3),
       '演示工程项目 ' || r."X",
       case mod(r."X", 7) when 0 then 'INITIATED' when 1 then 'BIDDING' when 2 then 'ENTRY' when 3 then 'CONSTRUCTION' when 4 then 'COMMISSIONING' when 5 then 'FINAL_ACCEPTANCE' else 'WARRANTY' end,
       case mod(r."X", 3) when 0 then 'NEW_CONSTRUCTION' when 1 then 'RENOVATION' else 'O_M_RENOVATION' end,
       '项目经理' || mod(r."X", 5), '上海市演示项目路 ' || r."X" || ' 号',
       150000 + r."X" * 10000, 30000 + r."X" * 2000, 10 + mod(r."X", 9) * 10,
       contract.amount, dateadd('MONTH', -1, current_date), dateadd('MONTH', 6, current_date),
       'APPROVED', '演示项目审批通过', '经营负责人', dateadd('MONTH', -1, current_timestamp),
       dateadd('YEAR', 2, current_date)
from system_range(1, 20) r
join crm_customers customer on customer.code = 'DEMO-CUST-' || right('000' || cast(r."X" as varchar), 3)
join crm_service_contracts contract on contract.code = 'DEMO-CONTRACT-' || right('000' || cast(r."X" as varchar), 3);

insert into project_budget_items (project_id, category, planned_amount, remark)
select project.id, case mod(r."X", 4) when 0 then 'LABOR' when 1 then 'MATERIAL' when 2 then 'SUBCONTRACT' else 'TRAVEL' end,
       project.budget_amount, '演示预算项'
from system_range(1, 20) r
join project_projects project on project.code = 'DEMO-PROJ-' || right('000' || cast(r."X" as varchar), 3);

insert into project_cost_entries (
  project_id, category, source_type, source_no, description, amount, incurred_date
)
select project.id, 'LABOR', 'MANUAL', 'DEMO-COST-' || right('000' || cast(r."X" as varchar), 3),
       '演示人工成本', 10000 + r."X" * 500, dateadd('DAY', -r."X", current_date)
from system_range(1, 20) r
join project_projects project on project.code = 'DEMO-PROJ-' || right('000' || cast(r."X" as varchar), 3);

insert into project_stage_records (
  project_id, from_stage, to_stage, progress, comment, operator_name, changed_at
)
select project.id, 'INITIATED', project.stage, project.progress, '演示项目阶段推进',
       project.manager_name, dateadd('DAY', -r."X", current_timestamp)
from system_range(1, 20) r
join project_projects project on project.code = 'DEMO-PROJ-' || right('000' || cast(r."X" as varchar), 3);

insert into inventory_parts (code, name, model, stock_qty, safety_qty, location, unit_cost)
select 'DEMO-PART-' || right('000' || cast(r."X" as varchar), 3), '演示备件 ' || r."X",
       'MODEL-' || right('000' || cast(r."X" as varchar), 3), 20 + r."X", 10,
       'A-' || right('00' || cast(r."X" as varchar), 2), 100 + r."X" * 10
from system_range(1, 20) r;

insert into procurement_suppliers (
  code, name, category, contact_name, phone, settlement_terms, risk_status
)
select 'DEMO-SUP-' || right('000' || cast(r."X" as varchar), 3), '演示供应商 ' || r."X",
       case mod(r."X", 3) when 0 then '电气设备' when 1 then '工程材料' else '服务外包' end,
       '供应商联系人' || r."X", '1360000' || right('0000' || cast(r."X" as varchar), 4), '月结30天',
       case when mod(r."X", 8) = 0 then 'WATCHLIST' else 'NORMAL' end
from system_range(1, 20) r;

insert into procurement_purchase_requests (
  code, requester_name, part_id, part_name, quantity, expected_date, reason,
  status, approval_status, cost_type, project_id, cost_target_code, cost_target_name
)
select 'DEMO-PR-' || right('000' || cast(r."X" as varchar), 3), '采购申请人' || r."X",
       part.id, part.name, 10 + r."X", dateadd('DAY', 10 + r."X", current_date),
       '演示项目物料采购', 'APPROVED', 'APPROVED', 'PROJECT', project.id, project.code, project.name
from system_range(1, 20) r
join inventory_parts part on part.code = 'DEMO-PART-' || right('000' || cast(r."X" as varchar), 3)
join project_projects project on project.code = 'DEMO-PROJ-' || right('000' || cast(r."X" as varchar), 3);

insert into procurement_request_approval_records (
  request_id, decision, comment, approver_name, decided_at
)
select request.id, 'APPROVED', '演示采购审批通过', '采购负责人', dateadd('DAY', -r."X", current_timestamp)
from system_range(1, 20) r
join procurement_purchase_requests request on request.code = 'DEMO-PR-' || right('000' || cast(r."X" as varchar), 3);

insert into procurement_purchase_orders (
  code, supplier_id, request_id, part_id, part_name, ordered_qty, received_qty,
  unit_price, order_amount, expected_delivery_date, status, cost_type, project_id,
  cost_target_code, cost_target_name
)
select 'DEMO-PO-' || right('000' || cast(r."X" as varchar), 3), supplier.id, request.id,
       part.id, part.name, request.quantity, request.quantity, part.unit_cost,
       request.quantity * part.unit_cost, request.expected_date, 'RECEIVED', 'PROJECT',
       project.id, project.code, project.name
from system_range(1, 20) r
join procurement_suppliers supplier on supplier.code = 'DEMO-SUP-' || right('000' || cast(r."X" as varchar), 3)
join procurement_purchase_requests request on request.code = 'DEMO-PR-' || right('000' || cast(r."X" as varchar), 3)
join inventory_parts part on part.id = request.part_id
join project_projects project on project.id = request.project_id;

insert into procurement_goods_receipts (
  code, order_id, part_id, quantity, unit_price, amount, received_date,
  delivery_no, receiver_name
)
select 'DEMO-GR-' || right('000' || cast(r."X" as varchar), 3), purchase_order.id, part.id,
       purchase_order.ordered_qty, purchase_order.unit_price, purchase_order.order_amount,
       dateadd('DAY', -r."X", current_date), 'DEMO-DN-' || right('000' || cast(r."X" as varchar), 3), '仓库管理员'
from system_range(1, 20) r
join procurement_purchase_orders purchase_order on purchase_order.code = 'DEMO-PO-' || right('000' || cast(r."X" as varchar), 3)
join inventory_parts part on part.id = purchase_order.part_id;

insert into fin_procurement_payables (
  code, supplier_id, order_id, receipt_id, amount, paid_amount, due_date, status
)
select 'DEMO-AP-' || right('000' || cast(r."X" as varchar), 3), supplier.id, purchase_order.id,
       receipt.id, receipt.amount, receipt.amount, dateadd('DAY', 30, receipt.received_date), 'PAID'
from system_range(1, 20) r
join procurement_suppliers supplier on supplier.code = 'DEMO-SUP-' || right('000' || cast(r."X" as varchar), 3)
join procurement_purchase_orders purchase_order on purchase_order.code = 'DEMO-PO-' || right('000' || cast(r."X" as varchar), 3)
join procurement_goods_receipts receipt on receipt.code = 'DEMO-GR-' || right('000' || cast(r."X" as varchar), 3);

insert into procurement_cost_allocations (
  order_id, receipt_id, cost_type, project_id, target_code, target_name,
  part_name, amount, incurred_date
)
select purchase_order.id, receipt.id, 'PROJECT', project.id, project.code, project.name,
       purchase_order.part_name, receipt.amount, receipt.received_date
from system_range(1, 20) r
join procurement_purchase_orders purchase_order on purchase_order.code = 'DEMO-PO-' || right('000' || cast(r."X" as varchar), 3)
join procurement_goods_receipts receipt on receipt.code = 'DEMO-GR-' || right('000' || cast(r."X" as varchar), 3)
join project_projects project on project.id = purchase_order.project_id;

insert into fin_payment_applications (
  code, payable_id, supplier_id, requested_amount, requested_date, applicant_name,
  purpose, status, approval_comment, approver_name, approved_at
)
select 'DEMO-PAYAPP-' || right('000' || cast(r."X" as varchar), 3), payable.id, supplier.id,
       payable.amount, dateadd('DAY', -r."X", current_date), '采购付款申请人',
       '演示采购货款支付', 'APPROVED', '演示付款审批通过', '财务负责人', dateadd('DAY', -r."X", current_timestamp)
from system_range(1, 20) r
join fin_procurement_payables payable on payable.code = 'DEMO-AP-' || right('000' || cast(r."X" as varchar), 3)
join procurement_suppliers supplier on supplier.code = 'DEMO-SUP-' || right('000' || cast(r."X" as varchar), 3);

insert into fin_payment_records (
  code, application_id, payable_id, supplier_id, amount, paid_date,
  payment_method, bank_reference, payer_name
)
select 'DEMO-PAY-' || right('000' || cast(r."X" as varchar), 3), application.id,
       payable.id, supplier.id, payable.amount, dateadd('DAY', -r."X", current_date),
       'BANK_TRANSFER', 'DEMO-BANK-' || right('000' || cast(r."X" as varchar), 3), '出纳员'
from system_range(1, 20) r
join fin_payment_applications application on application.code = 'DEMO-PAYAPP-' || right('000' || cast(r."X" as varchar), 3)
join fin_procurement_payables payable on payable.id = application.payable_id
join procurement_suppliers supplier on supplier.id = application.supplier_id;

update fin_payment_applications application
set payment_id = (select payment.id from fin_payment_records payment where payment.application_id = application.id),
    status = 'PAID'
where application.code like 'DEMO-PAYAPP-%';

insert into inventory_issue_orders (
  code, project_id, issue_date, receiver_name, purpose, total_amount, status
)
select 'DEMO-ISSUE-' || right('000' || cast(r."X" as varchar), 3), project.id,
       dateadd('DAY', -r."X", current_date), '项目领料人' || r."X", '演示项目施工领料',
       5 * part.unit_cost, 'PARTIAL_RETURNED'
from system_range(1, 20) r
join project_projects project on project.code = 'DEMO-PROJ-' || right('000' || cast(r."X" as varchar), 3)
join inventory_parts part on part.code = 'DEMO-PART-' || right('000' || cast(r."X" as varchar), 3);

insert into inventory_issue_lines (
  issue_id, part_id, part_name, quantity, returned_qty, unit_cost, amount
)
select issue.id, part.id, part.name, 5, 1, part.unit_cost, 5 * part.unit_cost
from system_range(1, 20) r
join inventory_issue_orders issue on issue.code = 'DEMO-ISSUE-' || right('000' || cast(r."X" as varchar), 3)
join inventory_parts part on part.code = 'DEMO-PART-' || right('000' || cast(r."X" as varchar), 3);

insert into inventory_return_orders (
  code, issue_id, project_id, return_date, handler_name, total_amount
)
select 'DEMO-RETURN-' || right('000' || cast(r."X" as varchar), 3), issue.id, issue.project_id,
       dateadd('DAY', 1 - r."X", current_date), '仓库管理员', part.unit_cost
from system_range(1, 20) r
join inventory_issue_orders issue on issue.code = 'DEMO-ISSUE-' || right('000' || cast(r."X" as varchar), 3)
join inventory_parts part on part.code = 'DEMO-PART-' || right('000' || cast(r."X" as varchar), 3);

insert into inventory_return_lines (
  return_id, issue_line_id, part_id, part_name, quantity, unit_cost, amount
)
select return_order.id, issue_line.id, part.id, part.name, 1, part.unit_cost, part.unit_cost
from system_range(1, 20) r
join inventory_return_orders return_order on return_order.code = 'DEMO-RETURN-' || right('000' || cast(r."X" as varchar), 3)
join inventory_issue_lines issue_line on issue_line.issue_id = return_order.issue_id
join inventory_parts part on part.id = issue_line.part_id;

insert into inventory_stock_movements (part_id, movement_type, quantity, source_no, remark)
select part.id, 'INBOUND', 10 + r."X", 'DEMO-GR-' || right('000' || cast(r."X" as varchar), 3), '演示采购入库'
from system_range(1, 20) r
join inventory_parts part on part.code = 'DEMO-PART-' || right('000' || cast(r."X" as varchar), 3);

insert into inventory_stock_movements (part_id, movement_type, quantity, source_no, remark)
select part.id, 'OUTBOUND', -5, 'DEMO-ISSUE-' || right('000' || cast(r."X" as varchar), 3), '演示项目领料'
from system_range(1, 20) r
join inventory_parts part on part.code = 'DEMO-PART-' || right('000' || cast(r."X" as varchar), 3);

insert into maintenance_equipment_assets (
  customer_id, contract_id, code, name, category, model, serial_no, site_address,
  installed_date, warranty_end_date, maintenance_cycle_days, last_maintenance_date,
  next_maintenance_date, status, required_certificate, notes
)
select customer.id, contract.id, 'DEMO-EQ-' || right('000' || cast(r."X" as varchar), 3),
       '演示配电设备 ' || r."X", '配电设备', 'EQ-MODEL-' || r."X",
       'DEMO-SN-' || right('000' || cast(r."X" as varchar), 3), '上海市演示现场 ' || r."X",
       dateadd('YEAR', -2, current_date), dateadd('YEAR', 1, current_date), 90,
       dateadd('DAY', -60, current_date), dateadd('DAY', 30 - r."X", current_date),
       case when r."X" > 15 then 'MAINTENANCE_DUE' else 'ACTIVE' end,
       case mod(r."X", 2) when 0 then '高压电工证' else '低压电工证' end, '本地演示设备'
from system_range(1, 20) r
join crm_customers customer on customer.code = 'DEMO-CUST-' || right('000' || cast(r."X" as varchar), 3)
join crm_service_contracts contract on contract.code = 'DEMO-CONTRACT-' || right('000' || cast(r."X" as varchar), 3);

insert into maintenance_plans (
  code, asset_id, contract_id, plan_name, cycle_days, next_due_date,
  last_generated_date, active
)
select 'DEMO-PLAN-' || right('000' || cast(r."X" as varchar), 3), equipment.id,
       equipment.contract_id, '季度预防性服务计划 ' || r."X", 90,
       dateadd('DAY', 30 - r."X", current_date), dateadd('DAY', -60, current_date), true
from system_range(1, 20) r
join maintenance_equipment_assets equipment on equipment.code = 'DEMO-EQ-' || right('000' || cast(r."X" as varchar), 3);

insert into work_orders (
  customer_id, contract_id, project_id, equipment_id, maintenance_plan_id,
  code, source, work_type, priority, status, title, equipment_name, planned_date,
  site_address, problem_description, assignee_id, engineer_name, required_cert,
  check_in_at, check_in_location, started_at, completed_at, accepted_at,
  customer_signer, service_result, acceptance_note, labor_hours, labor_cost,
  material_cost, travel_cost, outsourcing_cost, cost_amount, billable_amount, free_warranty
)
select equipment.customer_id, equipment.contract_id, project.id, equipment.id, plan.id,
       'DEMO-WO-' || right('000' || cast(r."X" as varchar), 3), 'MAINTENANCE_PLAN',
       case mod(r."X", 3) when 0 then 'INSPECTION' when 1 then 'REPAIR' else 'ON_SITE_SERVICE' end,
       case mod(r."X", 4) when 0 then 'LOW' when 1 then 'NORMAL' when 2 then 'HIGH' else 'URGENT' end,
       case mod(r."X", 5) when 0 then 'CREATED' when 1 then 'ASSIGNED' when 2 then 'IN_PROGRESS' when 3 then 'COMPLETED' else 'ACCEPTED' end,
       '演示服务工单 ' || r."X", equipment.name, dateadd('DAY', r."X" - 10, current_date),
       equipment.site_address, '例行巡检发现演示缺陷', users.id, users.display_name,
       equipment.required_certificate, dateadd('DAY', -r."X", current_timestamp), equipment.site_address,
       dateadd('DAY', -r."X", current_timestamp), dateadd('HOUR', 4, dateadd('DAY', -r."X", current_timestamp)),
       case when mod(r."X", 5) = 4 then dateadd('HOUR', 5, dateadd('DAY', -r."X", current_timestamp)) else null end,
       case when mod(r."X", 5) = 4 then '客户签字人' || r."X" else null end,
       '设备检查完成，运行状态正常', '演示验收通过', 4, 800, part.unit_cost, 200, 0,
       1000 + part.unit_cost, 3000 + r."X" * 100, false
from system_range(1, 20) r
join maintenance_equipment_assets equipment on equipment.code = 'DEMO-EQ-' || right('000' || cast(r."X" as varchar), 3)
join maintenance_plans plan on plan.code = 'DEMO-PLAN-' || right('000' || cast(r."X" as varchar), 3)
join project_projects project on project.code = 'DEMO-PROJ-' || right('000' || cast(r."X" as varchar), 3)
join sys_users users on users.username = 'demo' || right('000' || cast(r."X" as varchar), 3)
join inventory_parts part on part.code = 'DEMO-PART-' || right('000' || cast(r."X" as varchar), 3);

insert into work_order_materials (work_order_id, part_id, part_name, quantity, unit_cost, amount)
select work_order.id, part.id, part.name, 1, part.unit_cost, part.unit_cost
from system_range(1, 20) r
join work_orders work_order on work_order.code = 'DEMO-WO-' || right('000' || cast(r."X" as varchar), 3)
join inventory_parts part on part.code = 'DEMO-PART-' || right('000' || cast(r."X" as varchar), 3);

insert into work_order_status_logs (work_order_id, from_status, to_status, operator_name, remark)
select work_order.id, null, work_order.status, '系统管理员', '演示工单状态记录'
from system_range(1, 20) r
join work_orders work_order on work_order.code = 'DEMO-WO-' || right('000' || cast(r."X" as varchar), 3);

insert into hr_field_schedules (user_id, work_date, shift_name, site_name, status)
select users.id, dateadd('DAY', mod(r."X", 7), current_date), '白班', '演示项目现场 ' || r."X",
       case when mod(r."X", 6) = 0 then 'LEAVE' else 'ON_DUTY' end
from system_range(1, 20) r
join sys_users users on users.username = 'demo' || right('000' || cast(r."X" as varchar), 3);

insert into hr_field_attendance (
  user_id, work_order_id, check_in_at, check_out_at, check_in_location, check_out_location
)
select users.id, work_order.id, dateadd('DAY', -r."X", current_timestamp),
       dateadd('HOUR', 8, dateadd('DAY', -r."X", current_timestamp)),
       work_order.site_address, work_order.site_address
from system_range(1, 20) r
join sys_users users on users.username = 'demo' || right('000' || cast(r."X" as varchar), 3)
join work_orders work_order on work_order.code = 'DEMO-WO-' || right('000' || cast(r."X" as varchar), 3);

insert into oa_approval_requests (
  code, approval_type, title, source_no, amount, status, applicant_name,
  content, approver_name, approval_comment, processed_at
)
select 'DEMO-APR-' || right('000' || cast(r."X" as varchar), 3),
       case mod(r."X", 4) when 0 then 'EXPENSE' when 1 then 'PURCHASE' when 2 then 'OUTSOURCE' else 'PAYMENT' end,
       '演示审批事项 ' || r."X", 'DEMO-SOURCE-' || right('000' || cast(r."X" as varchar), 3),
       1000 + r."X" * 100, case when mod(r."X", 5) = 0 then 'PENDING' else 'APPROVED' end,
       '测试员工' || r."X", '演示审批业务内容',
       case when mod(r."X", 5) = 0 then null else '部门负责人' end,
       case when mod(r."X", 5) = 0 then null else '同意' end,
       case when mod(r."X", 5) = 0 then null else dateadd('DAY', -r."X", current_timestamp) end
from system_range(1, 20) r;

insert into oa_approval_actions (approval_id, decision, operator_name, comment)
select approval.id, case when approval.status = 'PENDING' then 'PENDING' else 'APPROVED' end,
       '部门负责人', case when approval.status = 'PENDING' then '等待处理' else '审批通过' end
from system_range(1, 20) r
join oa_approval_requests approval on approval.code = 'DEMO-APR-' || right('000' || cast(r."X" as varchar), 3);

insert into oa_expense_claims (
  code, claimant_id, claimant_name, project_id, work_order_id, expense_type,
  amount, expense_date, description, status, approval_request_id
)
select 'DEMO-EXP-' || right('000' || cast(r."X" as varchar), 3), users.id, users.display_name,
       project.id, work_order.id,
       case mod(r."X", 4) when 0 then 'TRAVEL' when 1 then 'TRANSPORT' when 2 then 'ACCOMMODATION' else 'TOOL' end,
       500 + r."X" * 50, dateadd('DAY', -r."X", current_date), '演示项目费用报销',
       case when mod(r."X", 5) = 0 then 'PENDING_APPROVAL' else 'APPROVED' end, approval.id
from system_range(1, 20) r
join sys_users users on users.username = 'demo' || right('000' || cast(r."X" as varchar), 3)
join project_projects project on project.code = 'DEMO-PROJ-' || right('000' || cast(r."X" as varchar), 3)
join work_orders work_order on work_order.code = 'DEMO-WO-' || right('000' || cast(r."X" as varchar), 3)
join oa_approval_requests approval on approval.code = 'DEMO-APR-' || right('000' || cast(r."X" as varchar), 3);

insert into oa_outsource_orders (
  code, supplier_id, project_id, work_order_id, service_type, description,
  amount, planned_date, status, approval_request_id, acceptance_note
)
select 'DEMO-OUT-' || right('000' || cast(r."X" as varchar), 3), supplier.id, project.id,
       work_order.id, '专项检测', '演示外包服务内容', 3000 + r."X" * 200,
       dateadd('DAY', r."X", current_date),
       case mod(r."X", 4) when 0 then 'PENDING_APPROVAL' when 1 then 'APPROVED' when 2 then 'IN_PROGRESS' else 'COMPLETED' end,
       approval.id, case when mod(r."X", 4) = 3 then '演示验收合格' else null end
from system_range(1, 20) r
join procurement_suppliers supplier on supplier.code = 'DEMO-SUP-' || right('000' || cast(r."X" as varchar), 3)
join project_projects project on project.code = 'DEMO-PROJ-' || right('000' || cast(r."X" as varchar), 3)
join work_orders work_order on work_order.code = 'DEMO-WO-' || right('000' || cast(r."X" as varchar), 3)
join oa_approval_requests approval on approval.code = 'DEMO-APR-' || right('000' || cast(r."X" as varchar), 3);

insert into doc_files (biz_type, biz_id, file_name, object_key, content_type, size_bytes)
select 'CONTRACT', contract.id, '演示合同附件-' || r."X" || '.pdf',
       'demo/contracts/demo-' || r."X" || '.pdf', 'application/pdf', 1024 + r."X" * 100
from system_range(1, 20) r
join crm_service_contracts contract on contract.code = 'DEMO-CONTRACT-' || right('000' || cast(r."X" as varchar), 3);

insert into system_notifications (
  type, title, content, target_user_id, related_type, related_id, dedup_key, is_read, read_at
)
select case mod(r."X", 3) when 0 then 'APPROVAL_RESULT' when 1 then 'EQUIPMENT' else 'FINANCE' end,
       '演示业务提醒 ' || r."X", '这是一条用于本地功能验证的演示提醒。', users.id,
       'WORK_ORDER', work_order.id, 'DEMO-NOTICE-' || right('000' || cast(r."X" as varchar), 3),
       mod(r."X", 4) = 0, case when mod(r."X", 4) = 0 then current_timestamp else null end
from system_range(1, 20) r
join sys_users users on users.username = 'demo' || right('000' || cast(r."X" as varchar), 3)
join work_orders work_order on work_order.code = 'DEMO-WO-' || right('000' || cast(r."X" as varchar), 3);

insert into operation_audits (
  username, http_method, request_path, response_status, client_ip, duration_ms, created_at
)
select 'demo' || right('000' || cast(r."X" as varchar), 3),
       case mod(r."X", 3) when 0 then 'GET' when 1 then 'POST' else 'PUT' end,
       '/api/demo/business/' || r."X", 200, '127.0.0.1', 20 + r."X",
       dateadd('MINUTE', -r."X", current_timestamp)
from system_range(1, 20) r;

insert into fin_accounting_vouchers (
  code, biz_type, biz_no, voucher_date, description, status, total_debit, total_credit
)
select 'DEMO-VOUCHER-' || right('000' || cast(r."X" as varchar), 3), 'DEMO_PAYMENT',
       'DEMO-PAY-' || right('000' || cast(r."X" as varchar), 3), dateadd('DAY', -r."X", current_date),
       '演示付款自动凭证 ' || r."X", 'POSTED', 1000 + r."X" * 100, 1000 + r."X" * 100
from system_range(1, 20) r;

insert into fin_accounting_entries (
  voucher_id, account_code, account_name, debit, credit, summary
)
select voucher.id, '2202', '应付账款', voucher.total_debit, 0, '演示付款冲销应付'
from system_range(1, 20) r
join fin_accounting_vouchers voucher on voucher.code = 'DEMO-VOUCHER-' || right('000' || cast(r."X" as varchar), 3);

insert into fin_accounting_entries (
  voucher_id, account_code, account_name, debit, credit, summary
)
select voucher.id, '1002', '银行存款', 0, voucher.total_credit, '演示银行付款'
from system_range(1, 20) r
join fin_accounting_vouchers voucher on voucher.code = 'DEMO-VOUCHER-' || right('000' || cast(r."X" as varchar), 3);
