alter table project_projects add column if not exists manager_user_id uuid;
alter table project_projects add column if not exists manager_assigned_by_user_id uuid;
alter table project_projects add column if not exists manager_assigned_by_name varchar(80);
alter table project_projects add column if not exists manager_assigned_at timestamp with time zone;
alter table project_projects add column if not exists manager_assignment_comment varchar(500);
alter table project_projects add column if not exists approver_user_id uuid;
alter table project_projects add column if not exists execution_status varchar(32) not null default 'ACTIVE';
alter table project_projects add column if not exists status_comment varchar(500);
alter table project_projects add column if not exists status_changed_at timestamp with time zone;

update project_projects set execution_status = case when stage = 'CLOSED' then 'CLOSED' else 'ACTIVE' end;

create index if not exists idx_project_manager_scope on project_projects (tenant_id, manager_user_id, created_at);
create index if not exists idx_project_execution_stage on project_projects (tenant_id, execution_status, stage, created_at);

create table if not exists biz_project_budget_version_items (
  id uuid default random_uuid() primary key,
  tenant_id varchar(64) not null default 'default',
  budget_version_id uuid not null references biz_project_budget_versions(id) on delete cascade,
  category varchar(40) not null,
  planned_amount decimal(14,2) not null,
  created_at timestamp with time zone default current_timestamp not null,
  updated_at timestamp with time zone default current_timestamp not null,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint default 0 not null,
  constraint uk_project_budget_version_item unique (tenant_id, budget_version_id, category)
);

create index if not exists idx_project_budget_version_item on biz_project_budget_version_items (tenant_id, budget_version_id, category);

alter table project_budget_items add constraint if not exists uk_project_budget_item_category unique (tenant_id, project_id, category);

create unique index if not exists uk_project_cost_source on project_cost_entries (tenant_id, source_type, source_no);
