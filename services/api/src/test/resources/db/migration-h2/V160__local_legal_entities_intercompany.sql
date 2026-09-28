-- 法人/账套与内部交易（H2 测试库镜像）：新表由 ddl-auto=update 按实体补齐
-- fin_accounting_vouchers / fin_accounting_entries.entity_id 为 DDL-only 预留字段（实体未映射），无需镜像
MERGE INTO fin_accounting_accounts (tenant_id,code,name,category,normal_direction,cash_account,active,system_account)
KEY(tenant_id,code) VALUES
 ('default','1221','内部往来','ASSET','DEBIT',false,true,true),
 ('default','2204','内部往来-应付','LIABILITY','CREDIT',false,true,true);
