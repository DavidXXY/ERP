-- 薪酬模块（H2 测试库镜像）：新表由 ddl-auto=update 按实体补齐，此处补齐会计科目
MERGE INTO fin_accounting_accounts (tenant_id,code,name,category,normal_direction,cash_account,active,system_account)
KEY(tenant_id,code) VALUES
 ('default','2212','其他应付款-社保公积金','LIABILITY','CREDIT',false,true,true),
 ('default','22210102','应交个人所得税','LIABILITY','CREDIT',false,true,true);
