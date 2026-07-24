alter table project_projects
  add column if not exists project_type varchar(40) not null default 'RENOVATION';

alter table project_projects
  add column if not exists manager_name varchar(80) not null default '历史项目负责人';

alter table project_projects
  add column if not exists site_address varchar(300) not null default '历史项目地址';

alter table project_projects
  add column if not exists contract_amount numeric(14, 2) not null default 0;

alter table project_projects
  add column if not exists planned_start_date date;

alter table project_projects
  add column if not exists planned_end_date date;

alter table project_projects
  add column if not exists approval_status varchar(32) not null default 'APPROVED';

alter table project_projects
  add column if not exists approval_comment varchar(500);

alter table project_projects
  add column if not exists approver_name varchar(80);

alter table project_projects
  add column if not exists approved_at timestamp;

update project_projects
set contract_amount = budget_amount,
    approval_status = 'APPROVED',
    approval_comment = '历史项目自动迁移',
    approver_name = '系统迁移',
    approved_at = current_timestamp
where contract_amount = 0;

create table if not exists project_budget_items (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  project_id uuid not null references project_projects(id),
  category varchar(40) not null,
  planned_amount numeric(14, 2) not null,
  remark varchar(300),
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64)
);

create table if not exists project_cost_entries (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  project_id uuid not null references project_projects(id),
  category varchar(40) not null,
  source_type varchar(40) not null,
  source_no varchar(80),
  description varchar(300) not null,
  amount numeric(14, 2) not null,
  incurred_date date not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64)
);

create table if not exists project_stage_records (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  project_id uuid not null references project_projects(id),
  from_stage varchar(40) not null,
  to_stage varchar(40) not null,
  progress integer not null,
  comment varchar(500) not null,
  operator_name varchar(80) not null,
  changed_at timestamp not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64)
);

insert into project_budget_items (tenant_id, project_id, category, planned_amount, remark)
select project.tenant_id, project.id, 'OTHER', project.budget_amount, '历史项目汇总预算'
from project_projects project
where not exists (
  select 1 from project_budget_items item where item.project_id = project.id
);

insert into project_cost_entries (
  tenant_id, project_id, category, source_type, source_no, description, amount, incurred_date
)
select project.tenant_id, project.id, 'OTHER', 'MANUAL', project.code, '历史项目汇总成本', project.actual_cost, current_date
from project_projects project
where project.actual_cost > 0
  and not exists (
    select 1 from project_cost_entries entry where entry.project_id = project.id
  );

create index if not exists idx_project_budget_project on project_budget_items (project_id, category);
create index if not exists idx_project_cost_project on project_cost_entries (project_id, incurred_date);
create index if not exists idx_project_stage_project on project_stage_records (project_id, changed_at);

merge into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) key (tenant_id, code) values
('00000000-0000-4000-8000-000000002501', 'default', 'project:approve', '项目立项审批', 'project', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002502', 'default', 'project:stage:update', '项目阶段推进', 'project', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002503', 'default', 'project:cost:create', '项目成本归集', 'project', current_timestamp, current_timestamp);

insert into sys_role_permissions (role_id, permission_id)
select role.id, permission.id
from sys_roles role
join sys_permissions permission on permission.tenant_id = role.tenant_id
where role.tenant_id = 'default'
  and role.code = 'ADMIN'
  and permission.code in ('project:approve', 'project:stage:update', 'project:cost:create')
  and not exists (
    select 1 from sys_role_permissions relation
    where relation.role_id = role.id and relation.permission_id = permission.id
  );
