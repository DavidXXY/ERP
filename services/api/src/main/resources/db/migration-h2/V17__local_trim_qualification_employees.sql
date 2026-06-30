delete from qual_employees
where external_id not in (
  'imp-dwy-0007', 'imp-dwy-0026', 'emp-photo-9c9bd760', 'imp-qt-0040', 'imp-qt-0001',
  'imp-qt-0048', 'imp-dwy-0040', 'imp-qt-0029', 'imp-qt-0061', 'imp-qt-0080'
);

drop index if exists idx_qual_employee_company;
alter table qual_employees drop column if exists subject_company;
create index if not exists idx_qual_employee_name on qual_employees (name);
