alter table crm_service_contracts
  add column if not exists quote_id uuid references crm_quote_plans(id);

create unique index if not exists uk_contract_quote on crm_service_contracts (quote_id)
where quote_id is not null;

alter table fin_receivables
  add column if not exists invoice_no varchar(80);

alter table fin_receivables
  add column if not exists invoice_date date;

alter table fin_receivables
  add column if not exists settled_amount numeric(14, 2) not null default 0;

update fin_receivables
set settled_amount = amount
where status = 'SETTLED' and settled_amount = 0;

create table if not exists crm_quote_approval_records (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  quote_id uuid not null references crm_quote_plans(id),
  decision varchar(32) not null,
  comment varchar(500) not null,
  approver_name varchar(80) not null,
  decided_at timestamptz not null,
  generated_contract_id uuid references crm_service_contracts(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64)
);

create index if not exists idx_quote_approval_quote on crm_quote_approval_records (quote_id, decided_at desc);

create table if not exists fin_receivable_receipts (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  receivable_id uuid not null references fin_receivables(id),
  amount numeric(14, 2) not null,
  received_date date not null,
  reference_no varchar(80) not null,
  recorder_name varchar(80) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64)
);

create index if not exists idx_receipt_receivable on fin_receivable_receipts (receivable_id, received_date desc);

insert into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) values
('00000000-0000-4000-8000-000000002311', 'default', 'crm:quote:approve', '报价审批处理', 'crm', now(), now()),
('00000000-0000-4000-8000-000000002312', 'default', 'crm:receivable:invoice', '应收开票登记', 'crm', now(), now()),
('00000000-0000-4000-8000-000000002313', 'default', 'crm:receivable:settle', '应收回款登记', 'crm', now(), now())
on conflict (tenant_id, code) do update
set name = excluded.name,
    module = excluded.module,
    updated_at = excluded.updated_at;

insert into sys_role_permissions (role_id, permission_id)
select r.id, p.id
from sys_roles r
join sys_permissions p on p.tenant_id = r.tenant_id
where r.tenant_id = 'default'
  and r.code in ('ADMIN', 'CRM_MANAGER')
  and p.code = 'crm:quote:approve'
on conflict do nothing;

insert into sys_role_permissions (role_id, permission_id)
select r.id, p.id
from sys_roles r
join sys_permissions p on p.tenant_id = r.tenant_id
where r.tenant_id = 'default'
  and r.code = 'ADMIN'
  and p.code in ('crm:receivable:invoice', 'crm:receivable:settle')
on conflict do nothing;
