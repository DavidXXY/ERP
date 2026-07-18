create table if not exists oa_expense_claim_lines (
  id uuid primary key,
  tenant_id varchar(64) not null default 'default',
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone,
  created_by varchar(64),
  updated_by varchar(64),
  expense_id uuid not null references oa_expense_claims(id) on delete cascade,
  line_no integer not null,
  expense_type varchar(40) not null,
  amount numeric(14,2) not null,
  expense_date date not null,
  description varchar(500) not null,
  invoice_file_name varchar(240),
  invoice_content_type varchar(120),
  invoice_size_bytes bigint
);

create index if not exists idx_expense_claim_lines_expense on oa_expense_claim_lines(expense_id, line_no);

insert into oa_expense_claim_lines (
  id, tenant_id, created_at, updated_at, expense_id, line_no, expense_type, amount, expense_date, description
)
select
  gen_random_uuid(), tenant_id, created_at, updated_at, id, 1, expense_type, amount, expense_date, description
from oa_expense_claims e
where not exists (
  select 1 from oa_expense_claim_lines l where l.expense_id = e.id
);
