-- H2 镜像：编号序列租户隔离
alter table code_sequences
  add column if not exists tenant_id varchar(64) not null default 'default';

alter table code_sequences drop primary key;
alter table code_sequences
  add constraint code_sequences_pkey primary key (entity_type, tenant_id);