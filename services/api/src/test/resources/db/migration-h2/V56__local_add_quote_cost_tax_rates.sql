alter table crm_quote_cost_requests add column if not exists labor_tax_rate numeric(5,2) not null default 6;
alter table crm_quote_cost_requests add column if not exists material_tax_rate numeric(5,2) not null default 13;
alter table crm_quote_cost_requests add column if not exists subcontract_tax_rate numeric(5,2) not null default 13;
alter table crm_quote_cost_requests add column if not exists travel_tax_rate numeric(5,2) not null default 0;
alter table crm_quote_cost_requests add column if not exists equipment_tax_rate numeric(5,2) not null default 13;
alter table crm_quote_cost_requests add column if not exists risk_reserve_tax_rate numeric(5,2) not null default 0;
alter table crm_quote_cost_requests add column if not exists other_tax_rate numeric(5,2) not null default 13;
