alter table approval_assignee_configs add column if not exists version bigint not null default 0;
alter table crm_attachment add column if not exists version bigint not null default 0;
alter table crm_contract_changes add column if not exists version bigint not null default 0;
alter table crm_customer_contacts add column if not exists version bigint not null default 0;
alter table crm_customer_sites add column if not exists version bigint not null default 0;
alter table crm_customers add column if not exists version bigint not null default 0;
alter table crm_follow_ups add column if not exists version bigint not null default 0;
alter table crm_opportunities add column if not exists version bigint not null default 0;
alter table crm_quote_approval_records add column if not exists version bigint not null default 0;
alter table crm_quote_cost_requests add column if not exists version bigint not null default 0;
alter table crm_quote_plans add column if not exists version bigint not null default 0;
alter table crm_quote_revisions add column if not exists version bigint not null default 0;
alter table crm_service_contracts add column if not exists version bigint not null default 0;
alter table doc_files add column if not exists version bigint not null default 0;
alter table fin_payment_applications add column if not exists version bigint not null default 0;
alter table fin_payment_records add column if not exists version bigint not null default 0;
alter table fin_procurement_payables add column if not exists version bigint not null default 0;
alter table fin_receivable_receipts add column if not exists version bigint not null default 0;
alter table fin_receivables add column if not exists version bigint not null default 0;
alter table hr_emergency_contacts add column if not exists version bigint not null default 0;
alter table hr_employee_certificates add column if not exists version bigint not null default 0;
alter table hr_employee_contracts add column if not exists version bigint not null default 0;
alter table hr_employee_education add column if not exists version bigint not null default 0;
alter table hr_employee_lifecycle_records add column if not exists version bigint not null default 0;
alter table hr_employee_work_experience add column if not exists version bigint not null default 0;
alter table hr_leave_balances add column if not exists version bigint not null default 0;
alter table hr_leave_requests add column if not exists version bigint not null default 0;
alter table inventory_issue_lines add column if not exists version bigint not null default 0;
alter table inventory_issue_orders add column if not exists version bigint not null default 0;
alter table inventory_parts add column if not exists version bigint not null default 0;
alter table inventory_return_lines add column if not exists version bigint not null default 0;
alter table inventory_return_orders add column if not exists version bigint not null default 0;
alter table inventory_stock_movements add column if not exists version bigint not null default 0;
alter table maintenance_equipment_assets add column if not exists version bigint not null default 0;
alter table oa_approval_actions add column if not exists version bigint not null default 0;
alter table oa_approval_requests add column if not exists version bigint not null default 0;
alter table oa_approval_runtime_nodes add column if not exists version bigint not null default 0;
alter table oa_expense_claim_lines add column if not exists version bigint not null default 0;
alter table oa_expense_claims add column if not exists version bigint not null default 0;
alter table oa_outsource_orders add column if not exists version bigint not null default 0;
alter table procurement_cost_allocations add column if not exists version bigint not null default 0;
alter table procurement_goods_receipts add column if not exists version bigint not null default 0;
alter table procurement_purchase_orders add column if not exists version bigint not null default 0;
alter table procurement_purchase_requests add column if not exists version bigint not null default 0;
alter table procurement_request_approval_records add column if not exists version bigint not null default 0;
alter table procurement_suppliers add column if not exists version bigint not null default 0;
alter table project_budget_items add column if not exists version bigint not null default 0;
alter table project_cost_entries add column if not exists version bigint not null default 0;
alter table project_projects add column if not exists version bigint not null default 0;
alter table project_stage_records add column if not exists version bigint not null default 0;
alter table qual_company_qualifications add column if not exists version bigint not null default 0;
alter table qual_employees add column if not exists version bigint not null default 0;
alter table qual_performances add column if not exists version bigint not null default 0;
alter table qual_personnel_certificates add column if not exists version bigint not null default 0;
alter table risk_rule_configs add column if not exists version bigint not null default 0;
alter table risk_snapshots add column if not exists version bigint not null default 0;
alter table risk_workflow_actions add column if not exists version bigint not null default 0;
alter table risk_workflows add column if not exists version bigint not null default 0;
alter table sys_organizations add column if not exists version bigint not null default 0;
alter table sys_permissions add column if not exists version bigint not null default 0;
alter table sys_roles add column if not exists version bigint not null default 0;
alter table sys_users add column if not exists version bigint not null default 0;
alter table system_audit_logs add column if not exists version bigint not null default 0;
alter table system_notifications add column if not exists version bigint not null default 0;
alter table work_order_materials add column if not exists version bigint not null default 0;
alter table work_order_status_logs add column if not exists version bigint not null default 0;
alter table work_orders add column if not exists version bigint not null default 0;

-- Safety net for every BaseEntity table, including modules added after this list was created.
do $$
declare table_name text;
begin
  for table_name in
    select c.table_name
    from information_schema.columns c
    where c.table_schema = current_schema()
      and c.column_name = 'tenant_id'
      and exists (
        select 1 from information_schema.columns id_col
        where id_col.table_schema = c.table_schema
          and id_col.table_name = c.table_name
          and id_col.column_name = 'id'
      )
  loop
    execute format('alter table %I add column if not exists version bigint not null default 0', table_name);
  end loop;
end $$;
