ALTER TABLE fin_receivables ADD COLUMN tax_status varchar(24) DEFAULT 'NORMAL' NOT NULL;
ALTER TABLE fin_receivables ADD COLUMN tax_adjustment_reason varchar(500);
ALTER TABLE fin_receivables ADD COLUMN tax_adjusted_at timestamp with time zone;
ALTER TABLE fin_receivables ADD COLUMN tax_adjusted_by varchar(80);

ALTER TABLE procurement_supplier_invoices ADD COLUMN tax_status varchar(24) DEFAULT 'NORMAL' NOT NULL;
ALTER TABLE procurement_supplier_invoices ADD COLUMN tax_adjustment_reason varchar(500);
ALTER TABLE procurement_supplier_invoices ADD COLUMN tax_adjusted_at timestamp with time zone;
ALTER TABLE procurement_supplier_invoices ADD COLUMN tax_adjusted_by varchar(80);

CREATE TABLE fin_accounting_accounts (
  id uuid DEFAULT random_uuid() PRIMARY KEY,
  tenant_id varchar(64) DEFAULT 'default' NOT NULL,
  code varchar(32) NOT NULL,
  name varchar(120) NOT NULL,
  category varchar(24) NOT NULL,
  normal_direction varchar(12) NOT NULL,
  cash_account boolean DEFAULT false NOT NULL,
  active boolean DEFAULT true NOT NULL,
  system_account boolean DEFAULT false NOT NULL,
  created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT uk_accounting_account_tenant_code UNIQUE (tenant_id, code),
  CONSTRAINT ck_accounting_account_category CHECK (category IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
  CONSTRAINT ck_accounting_account_direction CHECK (normal_direction IN ('DEBIT','CREDIT'))
);

CREATE TABLE fin_account_opening_balances (
  id uuid DEFAULT random_uuid() PRIMARY KEY,
  tenant_id varchar(64) DEFAULT 'default' NOT NULL,
  fiscal_year integer NOT NULL,
  account_code varchar(32) NOT NULL,
  debit_balance numeric(14,2) DEFAULT 0 NOT NULL,
  credit_balance numeric(14,2) DEFAULT 0 NOT NULL,
  note varchar(500),
  created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT uk_opening_balance_tenant_year_account UNIQUE (tenant_id, fiscal_year, account_code),
  CONSTRAINT ck_opening_balance_year CHECK (fiscal_year BETWEEN 2000 AND 2200),
  CONSTRAINT ck_opening_balance_amount CHECK (debit_balance >= 0 AND credit_balance >= 0),
  CONSTRAINT ck_opening_balance_direction CHECK (debit_balance = 0 OR credit_balance = 0),
  CONSTRAINT fk_opening_balance_account FOREIGN KEY (tenant_id, account_code)
    REFERENCES fin_accounting_accounts (tenant_id, code)
);

CREATE INDEX idx_accounting_account_category ON fin_accounting_accounts (tenant_id, category, active);
CREATE INDEX idx_opening_balance_year ON fin_account_opening_balances (tenant_id, fiscal_year);
CREATE INDEX idx_receivable_tax_ledger ON fin_receivables (tenant_id, invoice_date, tax_status);
CREATE INDEX idx_supplier_invoice_tax_ledger ON procurement_supplier_invoices (tenant_id, invoice_date, tax_status);

MERGE INTO fin_accounting_accounts
    (tenant_id, code, name, category, normal_direction, cash_account, active, system_account)
KEY (tenant_id, code) VALUES
    ('default','1001','库存现金','ASSET','DEBIT',true,true,true),
    ('default','1002','银行存款','ASSET','DEBIT',true,true,true),
    ('default','1122','应收账款','ASSET','DEBIT',false,true,true),
    ('default','1405','库存商品','ASSET','DEBIT',false,true,true),
    ('default','1601','固定资产','ASSET','DEBIT',false,true,true),
    ('default','2202','应付账款','LIABILITY','CREDIT',false,true,true),
    ('default','2211','应付职工薪酬','LIABILITY','CREDIT',false,true,true),
    ('default','222101','应交增值税-销项税额','LIABILITY','CREDIT',false,true,true),
    ('default','22210101','应交增值税-进项税额','LIABILITY','DEBIT',false,true,true),
    ('default','3001','实收资本','EQUITY','CREDIT',false,true,true),
    ('default','3104','利润分配','EQUITY','CREDIT',false,true,true),
    ('default','6001','主营业务收入','REVENUE','CREDIT',false,true,true),
    ('default','6401','主营业务成本','EXPENSE','DEBIT',false,true,true),
    ('default','6601','销售费用','EXPENSE','DEBIT',false,true,true),
    ('default','6602','管理费用','EXPENSE','DEBIT',false,true,true),
    ('default','6603','财务费用','EXPENSE','DEBIT',false,true,true);

MERGE INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
KEY (tenant_id, code) VALUES
    ('00000000-0000-4000-8000-000000005009','default','finance:tax:view','税务台账查看','finance',current_timestamp,current_timestamp,true,0),
    ('00000000-0000-4000-8000-000000005010','default','finance:tax:manage','发票作废红冲','finance',current_timestamp,current_timestamp,true,0),
    ('00000000-0000-4000-8000-000000005011','default','finance:account:manage','会计科目与期初余额维护','finance',current_timestamp,current_timestamp,true,0);

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code IN ('ADMIN','FINANCE_MANAGER','FINANCE_ACCOUNTANT')
  AND permission.code IN ('finance:tax:view','finance:tax:manage','finance:account:manage')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permissions existing
    WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );
