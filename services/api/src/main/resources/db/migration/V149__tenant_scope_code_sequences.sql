-- 编号序列租户隔离：code_sequences 原主键仅 entity_type，所有租户共享计数器。
-- 改为 (entity_type, tenant_id) 复合主键，已有行回填默认租户。
ALTER TABLE code_sequences
  ADD COLUMN IF NOT EXISTS tenant_id varchar(64) NOT NULL DEFAULT 'default';

ALTER TABLE code_sequences DROP CONSTRAINT IF EXISTS code_sequences_pkey;
ALTER TABLE code_sequences
  ADD CONSTRAINT code_sequences_pkey PRIMARY KEY (entity_type, tenant_id);
