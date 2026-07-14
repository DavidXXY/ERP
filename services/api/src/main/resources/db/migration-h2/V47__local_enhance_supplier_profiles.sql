alter table procurement_suppliers
  add column if not exists legal_representative varchar(80),
  add column if not exists unified_social_credit_code varchar(80),
  add column if not exists registered_capital varchar(80),
  add column if not exists registered_address varchar(240),
  add column if not exists business_scope varchar(800),
  add column if not exists license_valid_to date,
  add column if not exists qualification_valid_to date,
  add column if not exists taxpayer_type varchar(80),
  add column if not exists bank_name varchar(120),
  add column if not exists bank_account varchar(120),
  add column if not exists admission_status varchar(40),
  add column if not exists remark varchar(1000);
