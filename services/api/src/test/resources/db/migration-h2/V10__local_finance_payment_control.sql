create table if not exists fin_payment_applications (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  payable_id uuid not null references fin_procurement_payables(id),
  supplier_id uuid not null references procurement_suppliers(id),
  requested_amount numeric(14, 2) not null,
  requested_date date not null,
  applicant_name varchar(80) not null,
  purpose varchar(300) not null,
  status varchar(32) not null default 'PENDING_APPROVAL',
  approval_comment varchar(500),
  approver_name varchar(80),
  approved_at timestamp,
  payment_id uuid,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table if not exists fin_payment_records (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  application_id uuid not null references fin_payment_applications(id),
  payable_id uuid not null references fin_procurement_payables(id),
  supplier_id uuid not null references procurement_suppliers(id),
  amount numeric(14, 2) not null,
  paid_date date not null,
  payment_method varchar(32) not null,
  bank_reference varchar(100) not null,
  payer_name varchar(80) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create index if not exists idx_payment_application_payable on fin_payment_applications (payable_id, status);
create index if not exists idx_payment_application_status on fin_payment_applications (status, requested_date);
create index if not exists idx_payment_record_payable on fin_payment_records (payable_id, paid_date);

merge into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) key (tenant_id, code) values
('00000000-0000-4000-8000-000000002701', 'default', 'finance:receivable:view', '财务应收查看', 'finance', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002702', 'default', 'finance:receivable:invoice', '财务开票登记', 'finance', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002703', 'default', 'finance:receivable:collect', '财务回款核销', 'finance', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002704', 'default', 'finance:payable:view', '财务应付查看', 'finance', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002705', 'default', 'finance:payment:apply', '付款申请新增', 'finance', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002706', 'default', 'finance:payment:approve', '付款申请审批', 'finance', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002707', 'default', 'finance:payment:execute', '财务付款执行', 'finance', current_timestamp, current_timestamp);

insert into sys_role_permissions (role_id, permission_id)
select role.id, permission.id
from sys_roles role
join sys_permissions permission on permission.tenant_id = role.tenant_id
where role.tenant_id = 'default'
  and role.code = 'ADMIN'
  and permission.code in (
    'finance:receivable:view', 'finance:receivable:invoice', 'finance:receivable:collect',
    'finance:payable:view', 'finance:payment:apply', 'finance:payment:approve', 'finance:payment:execute'
  )
  and not exists (
    select 1 from sys_role_permissions relation
    where relation.role_id = role.id and relation.permission_id = permission.id
  );
