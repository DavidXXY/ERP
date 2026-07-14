alter table crm_quote_plans
  add column if not exists labor_budget numeric(14, 2) not null default 0,
  add column if not exists material_budget numeric(14, 2) not null default 0,
  add column if not exists subcontract_budget numeric(14, 2) not null default 0,
  add column if not exists travel_budget numeric(14, 2) not null default 0,
  add column if not exists other_budget numeric(14, 2) not null default 0;

alter table crm_quote_revisions
  add column if not exists labor_budget numeric(14, 2) not null default 0,
  add column if not exists material_budget numeric(14, 2) not null default 0,
  add column if not exists subcontract_budget numeric(14, 2) not null default 0,
  add column if not exists travel_budget numeric(14, 2) not null default 0,
  add column if not exists other_budget numeric(14, 2) not null default 0;
