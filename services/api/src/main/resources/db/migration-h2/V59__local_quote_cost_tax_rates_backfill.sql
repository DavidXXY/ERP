alter table crm_quote_cost_requests add column if not exists labor_tax_rate numeric(5,2) not null default 6;
alter table crm_quote_cost_requests add column if not exists material_tax_rate numeric(5,2) not null default 13;
alter table crm_quote_cost_requests add column if not exists subcontract_tax_rate numeric(5,2) not null default 6;
alter table crm_quote_cost_requests add column if not exists equipment_tax_rate numeric(5,2) not null default 13;
update crm_quote_cost_requests set labor_tax_rate=coalesce(labor_tax_rate,6), material_tax_rate=coalesce(material_tax_rate,13), subcontract_tax_rate=coalesce(subcontract_tax_rate,6), equipment_tax_rate=coalesce(equipment_tax_rate,13);
