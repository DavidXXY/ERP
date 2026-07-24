create table if not exists fin_accounting_vouchers (
  id uuid default RANDOM_UUID() primary key, tenant_id varchar(64) not null default 'default',
  code varchar(64) not null, biz_type varchar(60) not null, biz_no varchar(80) not null,
  voucher_date date not null, description varchar(500) not null, status varchar(32) not null,
  total_debit numeric(14,2) not null, total_credit numeric(14,2) not null,
  created_at timestamp not null default current_timestamp, updated_at timestamp not null default current_timestamp,
  created_by varchar(64), updated_by varchar(64), unique(tenant_id,code), unique(tenant_id,biz_type,biz_no)
);
create table if not exists fin_accounting_entries (
  id uuid default RANDOM_UUID() primary key, tenant_id varchar(64) not null default 'default',
  voucher_id uuid not null references fin_accounting_vouchers(id), account_code varchar(32) not null,
  account_name varchar(120) not null, debit numeric(14,2) not null default 0,
  credit numeric(14,2) not null default 0, summary varchar(300),
  created_at timestamp not null default current_timestamp, updated_at timestamp not null default current_timestamp,
  created_by varchar(64), updated_by varchar(64)
);
create index if not exists idx_voucher_date on fin_accounting_vouchers(voucher_date,status);
create index if not exists idx_entry_account on fin_accounting_entries(account_code,voucher_id);
merge into sys_permissions(id,tenant_id,code,name,module,created_at,updated_at) key(tenant_id,code) values
('00000000-0000-4000-8000-000000003001','default','finance:ledger:view','财务总账查看','finance',current_timestamp,current_timestamp);
insert into sys_role_permissions(role_id,permission_id)
select role.id,permission.id from sys_roles role join sys_permissions permission on permission.tenant_id=role.tenant_id
where role.tenant_id='default' and role.code='ADMIN' and permission.code='finance:ledger:view'
and not exists(select 1 from sys_role_permissions relation where relation.role_id=role.id and relation.permission_id=permission.id);
