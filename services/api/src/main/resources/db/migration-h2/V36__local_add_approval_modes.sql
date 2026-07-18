alter table approval_assignee_configs add column if not exists approval_mode varchar(20) not null default 'PARALLEL';
alter table approval_assignee_configs add column if not exists sequence_no integer not null default 1;
