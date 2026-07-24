alter table qual_employees add column if not exists organization_id uuid;

alter table qual_employees add constraint fk_qual_employee_organization
  foreign key (organization_id) references sys_organizations(id);

create index if not exists idx_qual_employee_organization
  on qual_employees (organization_id, employment_status);

update qual_employees employee
set organization_id = (
  select organization.id
  from sys_organizations organization
  where organization.tenant_id = employee.tenant_id
    and organization.name = employee.department
  fetch first 1 row only
)
where employee.organization_id is null
  and employee.department is not null
  and employee.department <> ''
  and exists (
    select 1
    from sys_organizations organization
    where organization.tenant_id = employee.tenant_id
      and organization.name = employee.department
  );

update qual_employees employee
set organization_id = (
  select organization.id
  from sys_organizations organization
  where organization.tenant_id = employee.tenant_id
    and organization.code = 'TECHNICAL_SERVICE_DEPARTMENT'
)
where employee.organization_id is null
  and employee.department is not null
  and employee.department not in ('', '未分配');

update qual_employees employee
set department = (
  select organization.name
  from sys_organizations organization
  where organization.id = employee.organization_id
)
where employee.organization_id is not null;

update qual_employees
set department = null
where organization_id is null and department in ('', '未分配');
