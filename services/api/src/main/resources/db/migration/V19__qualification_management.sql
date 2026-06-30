create table if not exists qual_employees (
  id uuid default gen_random_uuid() primary key,
  tenant_id varchar(64) not null default 'default', external_id varchar(100) not null,
  subject_company varchar(160) not null, name varchar(80) not null, work_no varchar(64),
  department varchar(120), position_name varchar(120), id_card varchar(32), phone varchar(40),
  entry_date date, employment_status varchar(32) not null default 'ACTIVE', contract_start date,
  contract_end date, social_security_unit varchar(160), social_security_start date,
  social_security_end date, remark varchar(1000), created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64),
  unique (tenant_id, external_id)
);

create table if not exists qual_company_qualifications (
  id uuid default gen_random_uuid() primary key,
  tenant_id varchar(64) not null default 'default', external_id varchar(100) not null,
  subject_company varchar(160) not null, name varchar(200) not null, category varchar(100) not null,
  level_name varchar(160), certificate_no varchar(180), issuer varchar(240), issue_date date,
  valid_from date, valid_to date, annual_review_date date, renewal_date date,
  scope_text varchar(4000), project_types_json varchar(2000), holder_branch varchar(240),
  storage_location varchar(500), available_for_tender boolean not null default true,
  manual_status varchar(32) not null default 'NORMAL', locked boolean not null default false,
  attachments_json varchar(4000), remark varchar(1000), created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64),
  unique (tenant_id, external_id)
);

create table if not exists qual_personnel_certificates (
  id uuid default gen_random_uuid() primary key,
  tenant_id varchar(64) not null default 'default', external_id varchar(100) not null,
  employee_id uuid not null references qual_employees(id) on delete cascade,
  name varchar(200) not null, certificate_type varchar(100), certificate_no varchar(180),
  specialty varchar(180), company_registered boolean not null default false, issue_date date,
  valid_to date, review_date date, available_for_tender boolean not null default true,
  manual_status varchar(32) not null default 'NORMAL', locked boolean not null default false,
  attachments_json varchar(4000), remark varchar(1000), created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64),
  unique (tenant_id, external_id)
);

create table if not exists qual_performances (
  id uuid default gen_random_uuid() primary key,
  tenant_id varchar(64) not null default 'default', external_id varchar(100) not null,
  subject_company varchar(160) not null, name varchar(500) not null, client_name varchar(240),
  contract_no varchar(160), contract_date date, contract_amount varchar(100), project_type varchar(160),
  attachments_json varchar(4000), remark varchar(1000), created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(), created_by varchar(64), updated_by varchar(64),
  unique (tenant_id, external_id)
);

create index if not exists idx_qual_employee_company on qual_employees (subject_company, name);
create index if not exists idx_qual_company_expiry on qual_company_qualifications (valid_to, annual_review_date);
create index if not exists idx_qual_certificate_employee on qual_personnel_certificates (employee_id, valid_to);
create index if not exists idx_qual_certificate_specialty on qual_personnel_certificates (specialty, valid_to);
create index if not exists idx_qual_performance_type on qual_performances (project_type, subject_company);

insert into sys_permissions (id, tenant_id, code, name, module, created_at, updated_at) values
('00000000-0000-4000-8000-000000004101','default','qualification:view','资质管理查看','qualification',now(),now()),
('00000000-0000-4000-8000-000000004102','default','qualification:company:view','公司资质查看','qualification',now(),now()),
('00000000-0000-4000-8000-000000004103','default','qualification:company:manage','公司资质维护','qualification',now(),now()),
('00000000-0000-4000-8000-000000004104','default','qualification:employee:view','资质人员查看','qualification',now(),now()),
('00000000-0000-4000-8000-000000004105','default','qualification:employee:manage','资质人员维护','qualification',now(),now()),
('00000000-0000-4000-8000-000000004106','default','qualification:certificate:view','人员证书查看','qualification',now(),now()),
('00000000-0000-4000-8000-000000004107','default','qualification:certificate:manage','人员证书维护','qualification',now(),now()),
('00000000-0000-4000-8000-000000004108','default','qualification:performance:view','项目业绩查看','qualification',now(),now()),
('00000000-0000-4000-8000-000000004109','default','qualification:performance:manage','项目业绩维护','qualification',now(),now()),
('00000000-0000-4000-8000-000000004110','default','qualification:tender:view','投标查询查看','qualification',now(),now()),
('00000000-0000-4000-8000-000000004111','default','qualification:warning:view','资质预警查看','qualification',now(),now())
on conflict (tenant_id, code) do update set name = excluded.name, module = excluded.module, updated_at = excluded.updated_at;

insert into sys_role_permissions (role_id, permission_id)
select role.id, permission.id from sys_roles role join sys_permissions permission on permission.tenant_id = role.tenant_id
where role.tenant_id = 'default' and role.code = 'ADMIN' and permission.module = 'qualification'
on conflict do nothing;
