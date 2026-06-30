alter table crm_quote_plans
  add column if not exists version_no integer not null default 1;

alter table crm_quote_plans
  add column if not exists customer_decision varchar(32);

alter table crm_quote_plans
  add column if not exists customer_comment varchar(500);

alter table crm_quote_plans
  add column if not exists customer_decision_by varchar(80);

alter table crm_quote_plans
  add column if not exists customer_decided_at timestamptz;

alter table crm_quote_approval_records
  add column if not exists quote_version integer not null default 1;

update crm_quote_plans q
set status = 'CONVERTED',
    customer_decision = 'ACCEPTED',
    customer_comment = '历史数据迁移：已生成合同',
    customer_decision_by = '系统迁移',
    customer_decided_at = q.updated_at
where q.status = 'APPROVED'
  and exists (select 1 from crm_service_contracts c where c.quote_id = q.id);

create table if not exists crm_quote_revisions (
  id uuid primary key default gen_random_uuid(),
  tenant_id varchar(64) not null default 'default',
  quote_id uuid not null references crm_quote_plans(id),
  version_no integer not null,
  code varchar(64) not null,
  service_scope varchar(800) not null,
  inspect_cycle varchar(120),
  payment_nodes varchar(300),
  amount numeric(14, 2) not null,
  status varchar(40) not null,
  revision_note varchar(500) not null,
  editor_name varchar(80) not null,
  revised_at timestamptz not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by varchar(64),
  updated_by varchar(64),
  unique (quote_id, version_no)
);

insert into crm_quote_revisions (
  tenant_id, quote_id, version_no, code, service_scope, inspect_cycle, payment_nodes,
  amount, status, revision_note, editor_name, revised_at
)
select q.tenant_id, q.id, q.version_no, q.code, q.service_scope, q.inspect_cycle, q.payment_nodes,
       q.amount, q.status, '历史报价初始版本', '系统迁移', q.updated_at
from crm_quote_plans q
where not exists (
  select 1 from crm_quote_revisions r
  where r.quote_id = q.id and r.version_no = q.version_no
);

create index if not exists idx_quote_revision_quote
  on crm_quote_revisions (quote_id, version_no desc);

insert into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) values
('00000000-0000-4000-8000-000000002315', 'default', 'crm:quote:update', '报价方案修订', 'crm', now(), now()),
('00000000-0000-4000-8000-000000002316', 'default', 'crm:quote:customer-result', '报价客户结果登记', 'crm', now(), now()),
('00000000-0000-4000-8000-000000002317', 'default', 'crm:quote:convert', '报价转合同', 'crm', now(), now())
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
  and p.code in ('crm:quote:update', 'crm:quote:customer-result', 'crm:quote:convert')
on conflict do nothing;
