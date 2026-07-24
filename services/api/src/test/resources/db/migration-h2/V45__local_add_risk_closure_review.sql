alter table risk_workflows add column if not exists root_cause varchar(1000);
alter table risk_workflows add column if not exists responsible_department varchar(120);
alter table risk_workflows add column if not exists handling_hours integer;
alter table risk_workflows add column if not exists recurrence boolean;
alter table risk_workflows add column if not exists prevention_action varchar(1000);

alter table risk_workflow_actions add column if not exists root_cause varchar(1000);
alter table risk_workflow_actions add column if not exists responsible_department varchar(120);
alter table risk_workflow_actions add column if not exists handling_hours integer;
alter table risk_workflow_actions add column if not exists recurrence boolean;
alter table risk_workflow_actions add column if not exists prevention_action varchar(1000);
