CREATE TABLE biz_accounting_periods (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    fiscal_year integer NOT NULL,
    period_no integer NOT NULL,
    status varchar(24) DEFAULT 'OPEN' NOT NULL,
    opened_at timestamp with time zone DEFAULT now() NOT NULL,
    closing_started_at timestamp with time zone,
    closed_at timestamp with time zone,
    closed_by varchar(80),
    close_reason varchar(500),
    reopened_at timestamp with time zone,
    reopened_by varchar(80),
    reopen_reason varchar(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT uk_accounting_period_tenant UNIQUE (tenant_id, fiscal_year, period_no),
    CONSTRAINT ck_accounting_period_no CHECK (period_no BETWEEN 1 AND 12)
);

CREATE TABLE biz_bank_statement_lines (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    account_no_masked varchar(80) NOT NULL,
    transaction_date date NOT NULL,
    direction varchar(12) NOT NULL,
    amount numeric(14,2) NOT NULL,
    counterparty varchar(180),
    bank_reference varchar(120) NOT NULL,
    summary varchar(500),
    reconciliation_status varchar(24) DEFAULT 'UNMATCHED' NOT NULL,
    matched_biz_type varchar(60),
    matched_biz_id uuid,
    matched_biz_no varchar(100),
    matched_at timestamp with time zone,
    matched_by varchar(80),
    match_note varchar(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT uk_bank_line_tenant_ref UNIQUE (tenant_id, account_no_masked, bank_reference),
    CONSTRAINT ck_bank_line_amount CHECK (amount > 0),
    CONSTRAINT ck_bank_line_direction CHECK (direction IN ('IN','OUT'))
);

CREATE TABLE biz_control_records (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    control_code varchar(64) NOT NULL,
    control_type varchar(48) NOT NULL,
    business_domain varchar(32) NOT NULL,
    business_id uuid,
    business_no varchar(100),
    name varchar(180) NOT NULL,
    owner varchar(80) NOT NULL,
    status varchar(24) DEFAULT 'DRAFT' NOT NULL,
    risk_level varchar(16) DEFAULT 'LOW' NOT NULL,
    planned_start date,
    planned_end date,
    effective_from date,
    effective_to date,
    budget_amount numeric(14,2) DEFAULT 0 NOT NULL,
    committed_amount numeric(14,2) DEFAULT 0 NOT NULL,
    actual_amount numeric(14,2) DEFAULT 0 NOT NULL,
    forecast_amount numeric(14,2) DEFAULT 0 NOT NULL,
    progress_percent numeric(5,2) DEFAULT 0 NOT NULL,
    review_frequency_days integer,
    last_reviewed_on date,
    next_review_on date,
    details text,
    activated_at timestamp with time zone,
    completed_at timestamp with time zone,
    completed_by varchar(80),
    completion_note varchar(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT uk_control_record_tenant_code UNIQUE (tenant_id, control_code),
    CONSTRAINT ck_control_progress CHECK (progress_percent BETWEEN 0 AND 100),
    CONSTRAINT ck_control_amounts CHECK (budget_amount >= 0 AND committed_amount >= 0 AND actual_amount >= 0 AND forecast_amount >= 0)
);

CREATE TABLE biz_governance_action_logs (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    entity_type varchar(32) NOT NULL,
    entity_id uuid NOT NULL,
    entity_no varchar(100),
    action_type varchar(32) NOT NULL,
    from_status varchar(32),
    to_status varchar(32),
    operator_name varchar(80) NOT NULL,
    note varchar(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL
);

CREATE INDEX idx_control_record_type_status ON biz_control_records (tenant_id, control_type, status);
CREATE INDEX idx_control_record_business ON biz_control_records (tenant_id, business_domain, business_id);
CREATE INDEX idx_control_record_due ON biz_control_records (tenant_id, planned_end, next_review_on);
CREATE INDEX idx_bank_line_status_date ON biz_bank_statement_lines (tenant_id, reconciliation_status, transaction_date);
CREATE INDEX idx_governance_action_entity ON biz_governance_action_logs (tenant_id, entity_type, entity_id, created_at);

ALTER TABLE fin_accounting_vouchers ADD COLUMN reviewed_at timestamp with time zone;
ALTER TABLE fin_accounting_vouchers ADD COLUMN reviewed_by varchar(80);
ALTER TABLE fin_accounting_vouchers ADD COLUMN posted_at timestamp with time zone;
ALTER TABLE fin_accounting_vouchers ADD COLUMN posted_by varchar(80);
ALTER TABLE fin_accounting_vouchers ADD COLUMN reversed_at timestamp with time zone;
ALTER TABLE fin_accounting_vouchers ADD COLUMN reversed_by varchar(80);
ALTER TABLE fin_accounting_vouchers ADD COLUMN reversal_reason varchar(500);
ALTER TABLE fin_accounting_vouchers ADD COLUMN reversal_voucher_id uuid;

UPDATE fin_accounting_vouchers
SET posted_at = COALESCE(posted_at, created_at), posted_by = COALESCE(posted_by, '系统')
WHERE status = 'POSTED';

INSERT INTO sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, built_in, version)
VALUES
  ('00000000-0000-4000-8000-000000005001', 'default', 'governance:view', '经营治理查看', 'governance', now(), now(), true, 0),
  ('00000000-0000-4000-8000-000000005002', 'default', 'governance:manage', '经营治理维护', 'governance', now(), now(), true, 0),
  ('00000000-0000-4000-8000-000000005003', 'default', 'governance:period:close', '会计期间关账', 'governance', now(), now(), true, 0),
  ('00000000-0000-4000-8000-000000005004', 'default', 'governance:bank:reconcile', '银行流水对账', 'governance', now(), now(), true, 0),
  ('00000000-0000-4000-8000-000000005005', 'default', 'finance:voucher:create', '凭证草稿新增', 'finance', now(), now(), true, 0),
  ('00000000-0000-4000-8000-000000005006', 'default', 'finance:voucher:review', '凭证复核', 'finance', now(), now(), true, 0),
  ('00000000-0000-4000-8000-000000005007', 'default', 'finance:voucher:post', '凭证记账', 'finance', now(), now(), true, 0),
  ('00000000-0000-4000-8000-000000005008', 'default', 'finance:voucher:reverse', '凭证冲销', 'finance', now(), now(), true, 0)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code = 'ADMIN'
  AND permission.code IN ('governance:view','governance:manage','governance:period:close','governance:bank:reconcile',
                          'finance:voucher:create','finance:voucher:review','finance:voucher:post','finance:voucher:reverse')
ON CONFLICT DO NOTHING;
