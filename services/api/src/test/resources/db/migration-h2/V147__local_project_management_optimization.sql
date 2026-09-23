create table if not exists project_milestones (
  id uuid default random_uuid() primary key,
  tenant_id varchar(64) not null default 'default',
  project_id uuid not null,
  name varchar(180) not null,
  planned_date date,
  actual_date date,
  status varchar(32) not null default 'PENDING',
  sort_order int not null default 0,
  remark varchar(500),
  created_at timestamp with time zone not null default current_timestamp,
  updated_at timestamp with time zone not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint not null default 0
);

create index if not exists idx_project_milestones_project
  on project_milestones (tenant_id, project_id, sort_order);

create table if not exists project_risks (
  id uuid default random_uuid() primary key,
  tenant_id varchar(64) not null default 'default',
  project_id uuid not null,
  title varchar(180) not null,
  description varchar(1000),
  severity varchar(16) not null default 'MEDIUM',
  status varchar(32) not null default 'OPEN',
  owner_name varchar(80),
  owner_user_id uuid,
  due_date date,
  resolution varchar(1000),
  created_at timestamp with time zone not null default current_timestamp,
  updated_at timestamp with time zone not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint not null default 0
);

create index if not exists idx_project_risks_project
  on project_risks (tenant_id, project_id, status);

create index if not exists idx_project_risks_due_status
  on project_risks (tenant_id, due_date, status);

create index if not exists idx_project_milestones_planned_status
  on project_milestones (tenant_id, planned_date, status);

create index if not exists idx_project_planned_end
  on project_projects (tenant_id, approval_status, stage, planned_end_date);
