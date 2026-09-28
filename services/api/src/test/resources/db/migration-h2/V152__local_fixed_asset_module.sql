-- 固定资产模块（H2 测试库镜像）：新表由 ddl-auto=update 按实体补齐，此处补齐会计科目
MERGE INTO fin_accounting_accounts (tenant_id,code,name,category,normal_direction,cash_account,active,system_account)
KEY(tenant_id,code) VALUES
 ('default','1602','累计折旧','ASSET','CREDIT',false,true,true),
 ('default','1603','固定资产减值准备','ASSET','CREDIT',false,true,true),
 ('default','6711','资产处置损益','EXPENSE','DEBIT',false,true,true);
