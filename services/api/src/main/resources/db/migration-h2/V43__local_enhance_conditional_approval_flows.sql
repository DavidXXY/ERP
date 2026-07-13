alter table approval_assignee_configs add column if not exists business_type varchar(80);
alter table approval_assignee_configs add column if not exists project_code varchar(80);
alter table approval_assignee_configs add column if not exists supplier_risk varchar(40);
alter table approval_assignee_configs add column if not exists customer_level varchar(40);
alter table approval_assignee_configs add column if not exists priority integer not null default 100;
create index if not exists idx_approval_configs_flow_priority on approval_assignee_configs(flow_code, enabled, priority, sequence_no);

alter table oa_approval_requests add column if not exists department_name varchar(120);
alter table oa_approval_requests add column if not exists business_type varchar(80);
alter table oa_approval_requests add column if not exists project_code varchar(80);
alter table oa_approval_requests add column if not exists supplier_risk varchar(40);
alter table oa_approval_requests add column if not exists customer_level varchar(40);
alter table oa_approval_requests add column if not exists approval_mode varchar(20);
alter table oa_approval_requests add column if not exists current_step integer;
alter table oa_approval_requests add column if not exists total_steps integer;
alter table oa_approval_requests add column if not exists current_approver_name varchar(120);
alter table oa_approval_requests add column if not exists delegated_user_id uuid;
alter table oa_approval_requests add column if not exists matched_rule_text varchar(500);
create index if not exists idx_oa_approval_requests_context on oa_approval_requests(approval_type, status, business_type, department_name);

alter table oa_approval_actions add column if not exists action_type varchar(32) not null default 'APPROVE';
alter table oa_approval_actions add column if not exists step_no integer;
