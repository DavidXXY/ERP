-- H2 测试镜像：V150 的完整数据完整性/索引/精度变更针对 PostgreSQL 生产 schema 中的表，
-- 而 H2 测试 schema 为最小子集（由后续 ddl-auto=update 按实体补齐），这些表大多不存在。
-- 本镜像仅对 H2 schema 中确实存在的表应用等价的索引，其余变更对 H2 测试无影响。

create index if not exists idx_sys_organizations_parent on sys_organizations(parent_id);
create index if not exists idx_fin_receivables_contract on fin_receivables(contract_id);
create index if not exists idx_crm_contract_changes_contract on crm_contract_changes(contract_id);
