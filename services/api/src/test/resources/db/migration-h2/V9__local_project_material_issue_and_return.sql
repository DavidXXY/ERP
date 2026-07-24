create table if not exists inventory_issue_orders (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  project_id uuid not null references project_projects(id),
  issue_date date not null,
  receiver_name varchar(80) not null,
  purpose varchar(300) not null,
  total_amount numeric(14, 2) not null default 0,
  status varchar(32) not null default 'POSTED',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table if not exists inventory_issue_lines (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  issue_id uuid not null references inventory_issue_orders(id),
  part_id uuid not null references inventory_parts(id),
  part_name varchar(160) not null,
  quantity numeric(14, 2) not null,
  returned_qty numeric(14, 2) not null default 0,
  unit_cost numeric(14, 2) not null,
  amount numeric(14, 2) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64)
);

create table if not exists inventory_return_orders (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  code varchar(64) not null,
  issue_id uuid not null references inventory_issue_orders(id),
  project_id uuid not null references project_projects(id),
  return_date date not null,
  handler_name varchar(80) not null,
  total_amount numeric(14, 2) not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  unique (tenant_id, code)
);

create table if not exists inventory_return_lines (
  id uuid default RANDOM_UUID() primary key,
  tenant_id varchar(64) not null default 'default',
  return_id uuid not null references inventory_return_orders(id),
  issue_line_id uuid not null references inventory_issue_lines(id),
  part_id uuid not null references inventory_parts(id),
  part_name varchar(160) not null,
  quantity numeric(14, 2) not null,
  unit_cost numeric(14, 2) not null,
  amount numeric(14, 2) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  created_by varchar(64),
  updated_by varchar(64)
);

create index if not exists idx_inventory_issue_project on inventory_issue_orders (project_id, issue_date);
create index if not exists idx_inventory_issue_line_issue on inventory_issue_lines (issue_id, part_id);
create index if not exists idx_inventory_return_issue on inventory_return_orders (issue_id, return_date);
create index if not exists idx_inventory_return_line_return on inventory_return_lines (return_id, issue_line_id);

merge into sys_permissions (
  id, tenant_id, code, name, module, created_at, updated_at
) key (tenant_id, code) values
('00000000-0000-4000-8000-000000002601', 'default', 'inventory:issue:create', '项目领料办理', 'inventory', current_timestamp, current_timestamp),
('00000000-0000-4000-8000-000000002602', 'default', 'inventory:return:create', '项目退料办理', 'inventory', current_timestamp, current_timestamp);

insert into sys_role_permissions (role_id, permission_id)
select role.id, permission.id
from sys_roles role
join sys_permissions permission on permission.tenant_id = role.tenant_id
where role.tenant_id = 'default'
  and role.code = 'ADMIN'
  and permission.code in ('inventory:issue:create', 'inventory:return:create')
  and not exists (
    select 1 from sys_role_permissions relation
    where relation.role_id = role.id and relation.permission_id = permission.id
  );
