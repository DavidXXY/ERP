alter table system_audit_logs add column if not exists query_string varchar(1000);
alter table system_audit_logs add column if not exists operation_type varchar(40);
alter table system_audit_logs add column if not exists biz_module varchar(80);
alter table system_audit_logs add column if not exists biz_object varchar(120);
create index if not exists idx_system_audit_logs_module_created on system_audit_logs(biz_module, created_at desc);
create index if not exists idx_system_audit_logs_operation_created on system_audit_logs(operation_type, created_at desc);
