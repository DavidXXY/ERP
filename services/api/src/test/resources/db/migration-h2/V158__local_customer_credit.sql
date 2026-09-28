-- 客户信用管控（H2 测试库镜像）：生产 V158 对 crm_customers 增加信用额度与冻结标记

ALTER TABLE crm_customers ADD COLUMN IF NOT EXISTS credit_limit numeric(18,2);
ALTER TABLE crm_customers ADD COLUMN IF NOT EXISTS credit_blocked boolean NOT NULL DEFAULT false;