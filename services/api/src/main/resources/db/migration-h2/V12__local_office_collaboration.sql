create table if not exists oa_approval_requests (
  id uuid default RANDOM_UUID() primary key, tenant_id varchar(64) not null default 'default',
  code varchar(64) not null, approval_type varchar(80) not null, title varchar(180) not null,
  source_no varchar(64), amount numeric(14,2), status varchar(40) not null default 'PENDING',
  applicant_name varchar(80), content varchar(1000), approver_name varchar(80),
  approval_comment varchar(500), processed_at timestamp, created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp, created_by varchar(64), updated_by varchar(64),
  unique (tenant_id, code)
);
alter table oa_approval_requests add column if not exists content varchar(1000);
alter table oa_approval_requests add column if not exists approver_name varchar(80);
alter table oa_approval_requests add column if not exists approval_comment varchar(500);
alter table oa_approval_requests add column if not exists processed_at timestamp;

create table if not exists oa_approval_actions (
  id uuid default RANDOM_UUID() primary key, tenant_id varchar(64) not null default 'default',
  approval_id uuid not null references oa_approval_requests(id), decision varchar(40) not null,
  operator_name varchar(80) not null, comment varchar(500) not null,
  created_at timestamp not null default current_timestamp, updated_at timestamp not null default current_timestamp,
  created_by varchar(64), updated_by varchar(64)
);

create table if not exists oa_expense_claims (
  id uuid default RANDOM_UUID() primary key, tenant_id varchar(64) not null default 'default',
  code varchar(64) not null, claimant_id uuid references sys_users(id), claimant_name varchar(80) not null,
  project_id uuid references project_projects(id), work_order_id uuid references work_orders(id),
  expense_type varchar(40) not null, amount numeric(14,2) not null, expense_date date not null,
  description varchar(500) not null, status varchar(40) not null,
  approval_request_id uuid references oa_approval_requests(id), created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp, created_by varchar(64), updated_by varchar(64),
  unique (tenant_id, code)
);

create table if not exists oa_outsource_orders (
  id uuid default RANDOM_UUID() primary key, tenant_id varchar(64) not null default 'default',
  code varchar(64) not null, supplier_id uuid not null references procurement_suppliers(id),
  project_id uuid references project_projects(id), work_order_id uuid references work_orders(id),
  service_type varchar(100) not null, description varchar(800) not null, amount numeric(14,2) not null,
  planned_date date not null, status varchar(40) not null, approval_request_id uuid references oa_approval_requests(id),
  acceptance_note varchar(500), created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp, created_by varchar(64), updated_by varchar(64),
  unique (tenant_id, code)
);

create table if not exists doc_files (
  id uuid default RANDOM_UUID() primary key, tenant_id varchar(64) not null default 'default',
  biz_type varchar(80) not null, biz_id uuid, file_name varchar(240) not null,
  object_key varchar(500) not null, content_type varchar(120), size_bytes bigint,
  created_at timestamp not null default current_timestamp, updated_at timestamp not null default current_timestamp,
  created_by varchar(64), updated_by varchar(64)
);

create table if not exists system_notifications (
  id uuid default RANDOM_UUID() primary key, tenant_id varchar(64) not null default 'default',
  type varchar(40) not null, title varchar(180) not null, content varchar(1000) not null,
  target_user_id uuid references sys_users(id), related_type varchar(80), related_id uuid,
  dedup_key varchar(180),
  is_read boolean not null default false, read_at timestamp,
  created_at timestamp not null default current_timestamp, updated_at timestamp not null default current_timestamp,
  created_by varchar(64), updated_by varchar(64)
);

create table if not exists operation_audits (
  id uuid default RANDOM_UUID() primary key, tenant_id varchar(64) not null default 'default',
  username varchar(80), http_method varchar(12) not null, request_path varchar(500) not null,
  response_status int not null, client_ip varchar(80), duration_ms bigint not null,
  created_at timestamp not null default current_timestamp, updated_at timestamp not null default current_timestamp,
  created_by varchar(64), updated_by varchar(64)
);

create index if not exists idx_approval_status on oa_approval_requests (status, created_at);
create index if not exists idx_expense_project on oa_expense_claims (project_id, status);
create index if not exists idx_outsource_status on oa_outsource_orders (status, planned_date);
create index if not exists idx_document_biz on doc_files (biz_type, biz_id);
create index if not exists idx_notification_read on system_notifications (is_read, created_at);
create unique index if not exists uk_notification_dedup on system_notifications (tenant_id, dedup_key);
create index if not exists idx_operation_audit_created on operation_audits (created_at);

merge into sys_permissions (id, tenant_id, code, name, module, created_at, updated_at) key (tenant_id, code) values
('00000000-0000-4000-8000-000000002901','default','office:view','OA协同查看','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002902','default','office:approval:view','业务待办审批查看','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002903','default','office:approval:create','审批申请新增','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002904','default','office:approval:process','审批处理','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002905','default','office:expense:view','费用报销查看','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002906','default','office:expense:create','费用报销新增','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002907','default','office:outsource:view','外包服务查看','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002908','default','office:outsource:create','外包服务新增','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002909','default','office:outsource:complete','外包服务验收','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002910','default','office:document:view','电子档案查看','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002911','default','office:document:upload','电子档案上传','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002912','default','office:notification:view','消息中心查看','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002913','default','office:audit:view','操作审计查看','office',current_timestamp,current_timestamp),
('00000000-0000-4000-8000-000000002914','default','office:document:delete','电子档案删除','office',current_timestamp,current_timestamp);

insert into sys_role_permissions (role_id, permission_id)
select role.id, permission.id from sys_roles role join sys_permissions permission on permission.tenant_id=role.tenant_id
where role.tenant_id='default' and role.code='ADMIN' and permission.module='office'
and not exists (select 1 from sys_role_permissions relation where relation.role_id=role.id and relation.permission_id=permission.id);
