-- 预收/预付（H2 测试库镜像）：新表由 ddl-auto=update 按实体补齐，此处补齐会计科目
MERGE INTO fin_accounting_accounts (tenant_id,code,name,category,normal_direction,cash_account,active,system_account)
KEY(tenant_id,code) VALUES
 ('default','2203','预收账款','LIABILITY','CREDIT',false,true,true),
 ('default','1123','预付账款','ASSET','DEBIT',false,true,true);
