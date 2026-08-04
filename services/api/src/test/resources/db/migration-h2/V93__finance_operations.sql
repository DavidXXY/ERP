CREATE TABLE fin_period_end_jobs (
  id uuid DEFAULT random_uuid() PRIMARY KEY, tenant_id varchar(64) DEFAULT 'default' NOT NULL,
  fiscal_year integer NOT NULL, period_no integer NOT NULL, process_type varchar(32) NOT NULL,
  description varchar(300) NOT NULL, amount numeric(14,2) NOT NULL, debit_account_code varchar(32) NOT NULL,
  credit_account_code varchar(32) NOT NULL, auto_reverse boolean DEFAULT false NOT NULL, reversal_date date,
  status varchar(24) DEFAULT 'PENDING' NOT NULL, voucher_id uuid, reversal_voucher_id uuid,
  idempotency_key varchar(100) NOT NULL, executed_at timestamp with time zone, executed_by varchar(80),
  created_at timestamp with time zone DEFAULT current_timestamp NOT NULL, updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT uk_period_job_key UNIQUE (tenant_id,idempotency_key), CONSTRAINT ck_period_job_period CHECK(period_no BETWEEN 1 AND 12),
  CONSTRAINT ck_period_job_amount CHECK(amount>0), CONSTRAINT ck_period_job_type CHECK(process_type IN ('ACCRUAL','AMORTIZATION','DEPRECIATION','PROFIT_CARRY_FORWARD')),
  CONSTRAINT ck_period_job_status CHECK(status IN ('PENDING','COMPLETED','REVERSED','FAILED'))
);
CREATE TABLE fin_partner_reconciliations (
  id uuid DEFAULT random_uuid() PRIMARY KEY, tenant_id varchar(64) DEFAULT 'default' NOT NULL, partner_type varchar(16) NOT NULL,
  partner_id uuid NOT NULL, partner_name varchar(180) NOT NULL, period_end date NOT NULL, ledger_balance numeric(14,2) NOT NULL,
  statement_balance numeric(14,2) NOT NULL, difference_amount numeric(14,2) NOT NULL, status varchar(24) NOT NULL,
  confirmation_note varchar(1000), confirmed_at timestamp with time zone, confirmed_by varchar(80),
  created_at timestamp with time zone DEFAULT current_timestamp NOT NULL, updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT uk_partner_recon UNIQUE(tenant_id,partner_type,partner_id,period_end),
  CONSTRAINT ck_partner_recon_type CHECK(partner_type IN ('CUSTOMER','SUPPLIER')), CONSTRAINT ck_partner_recon_status CHECK(status IN ('PENDING','MATCHED','DISPUTED','CONFIRMED'))
);
CREATE TABLE fin_cash_forecast_scenarios (
  id uuid DEFAULT random_uuid() PRIMARY KEY, tenant_id varchar(64) DEFAULT 'default' NOT NULL, name varchar(120) NOT NULL,
  as_of_date date NOT NULL, horizon_days integer NOT NULL, opening_cash numeric(14,2) NOT NULL, expected_receipts numeric(14,2) NOT NULL,
  expected_payments numeric(14,2) NOT NULL, receipt_adjustment numeric(14,2) DEFAULT 0 NOT NULL, payment_adjustment numeric(14,2) DEFAULT 0 NOT NULL,
  forecast_cash numeric(14,2) NOT NULL, status varchar(24) DEFAULT 'DRAFT' NOT NULL, assumptions varchar(2000),
  created_at timestamp with time zone DEFAULT current_timestamp NOT NULL, updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT ck_cash_scenario_horizon CHECK(horizon_days BETWEEN 1 AND 3660), CONSTRAINT ck_cash_scenario_status CHECK(status IN ('DRAFT','APPROVED','ARCHIVED'))
);
CREATE TABLE fin_tax_filings (
  id uuid DEFAULT random_uuid() PRIMARY KEY, tenant_id varchar(64) DEFAULT 'default' NOT NULL, fiscal_year integer NOT NULL, period_no integer NOT NULL,
  output_tax numeric(14,2) NOT NULL, input_tax numeric(14,2) NOT NULL, tax_payable numeric(14,2) NOT NULL, ledger_tax numeric(14,2) NOT NULL,
  difference_amount numeric(14,2) NOT NULL, status varchar(24) DEFAULT 'DRAFT' NOT NULL, filing_reference varchar(100), locked_at timestamp with time zone,
  locked_by varchar(80), snapshot_id uuid, created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL, created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT uk_tax_filing_period UNIQUE(tenant_id,fiscal_year,period_no), CONSTRAINT ck_tax_filing_period CHECK(period_no BETWEEN 1 AND 12),
  CONSTRAINT ck_tax_filing_status CHECK(status IN ('DRAFT','RECONCILED','FILED','LOCKED'))
);
CREATE TABLE fin_consolidation_runs (
  id uuid DEFAULT random_uuid() PRIMARY KEY, tenant_id varchar(64) DEFAULT 'default' NOT NULL, fiscal_year integer NOT NULL, period_no integer NOT NULL,
  name varchar(160) NOT NULL, entity_count integer NOT NULL, combined_revenue numeric(14,2) NOT NULL, combined_expense numeric(14,2) NOT NULL,
  intercompany_revenue numeric(14,2) DEFAULT 0 NOT NULL, intercompany_expense numeric(14,2) DEFAULT 0 NOT NULL,
  consolidated_profit numeric(14,2) NOT NULL, entity_payload varchar(10000) NOT NULL, status varchar(24) DEFAULT 'DRAFT' NOT NULL,
  snapshot_id uuid, completed_at timestamp with time zone, completed_by varchar(80), created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL, created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT ck_consolidation_period CHECK(period_no BETWEEN 1 AND 12), CONSTRAINT ck_consolidation_entities CHECK(entity_count>=2),
  CONSTRAINT ck_consolidation_status CHECK(status IN ('DRAFT','COMPLETED'))
);
CREATE TABLE fin_report_snapshots (
  id uuid DEFAULT random_uuid() PRIMARY KEY, tenant_id varchar(64) DEFAULT 'default' NOT NULL, report_type varchar(48) NOT NULL,
  scope_key varchar(120) NOT NULL, fiscal_year integer, period_no integer, payload varchar(20000) NOT NULL, content_hash varchar(64) NOT NULL,
  evidence_note varchar(1000), captured_at timestamp with time zone DEFAULT current_timestamp NOT NULL, captured_by varchar(80) NOT NULL,
  created_at timestamp with time zone DEFAULT current_timestamp NOT NULL, updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT uk_report_snapshot_hash UNIQUE(tenant_id,report_type,scope_key,content_hash)
);
CREATE TABLE fin_voucher_generation_requests (
  id uuid DEFAULT random_uuid() PRIMARY KEY, tenant_id varchar(64) DEFAULT 'default' NOT NULL, idempotency_key varchar(100) NOT NULL,
  source_type varchar(48) NOT NULL, business_no varchar(100) NOT NULL, status varchar(24) DEFAULT 'PENDING' NOT NULL,
  attempt_count integer DEFAULT 0 NOT NULL, voucher_id uuid, last_error varchar(1000), last_attempt_at timestamp with time zone,
  completed_at timestamp with time zone, created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL, created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT uk_voucher_request_key UNIQUE(tenant_id,idempotency_key), CONSTRAINT ck_voucher_request_status CHECK(status IN ('PENDING','PROCESSING','SUCCEEDED','FAILED','COMPENSATED'))
);
CREATE INDEX idx_period_jobs_period ON fin_period_end_jobs(tenant_id,fiscal_year,period_no,status);
CREATE INDEX idx_partner_recon_period ON fin_partner_reconciliations(tenant_id,period_end,status);
CREATE INDEX idx_cash_scenario_asof ON fin_cash_forecast_scenarios(tenant_id,as_of_date);
CREATE INDEX idx_tax_filing_period ON fin_tax_filings(tenant_id,fiscal_year,period_no,status);
CREATE INDEX idx_consolidation_period ON fin_consolidation_runs(tenant_id,fiscal_year,period_no);
CREATE INDEX idx_report_snapshot_scope ON fin_report_snapshots(tenant_id,report_type,scope_key,captured_at);
CREATE INDEX idx_voucher_request_status ON fin_voucher_generation_requests(tenant_id,status,last_attempt_at);
MERGE INTO fin_accounting_accounts (tenant_id,code,name,category,normal_direction,cash_account,active,system_account) KEY(tenant_id,code) VALUES
 ('default','1602','累计折旧','ASSET','CREDIT',false,true,true),('default','1701','无形资产','ASSET','DEBIT',false,true,true),
 ('default','1702','累计摊销','ASSET','CREDIT',false,true,true),('default','2201','应付账款-暂估','LIABILITY','CREDIT',false,true,true);
MERGE INTO sys_permissions (id,tenant_id,code,name,module,created_at,updated_at,built_in,version) KEY(tenant_id,code) VALUES
 ('00000000-0000-4000-8000-000000005012','default','finance:operations:view','财务运营工作台查看','finance',current_timestamp,current_timestamp,true,0),
 ('00000000-0000-4000-8000-000000005013','default','finance:operations:manage','财务运营处理','finance',current_timestamp,current_timestamp,true,0);
INSERT INTO sys_role_permissions(role_id,permission_id) SELECT r.id,p.id FROM sys_roles r JOIN sys_permissions p ON p.tenant_id=r.tenant_id
 WHERE r.code IN('ADMIN','FINANCE_MANAGER','FINANCE_ACCOUNTANT') AND p.code IN('finance:operations:view','finance:operations:manage')
 AND NOT EXISTS(SELECT 1 FROM sys_role_permissions x WHERE x.role_id=r.id AND x.permission_id=p.id);
