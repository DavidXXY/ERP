alter table hr_leave_balances add column if not exists leave_year integer;
update hr_leave_balances set leave_year=coalesce(leave_year, year, year(current_date)) where leave_year is null;
alter table hr_leave_balances alter column leave_year set not null;
alter table hr_leave_balances drop column if exists year;
