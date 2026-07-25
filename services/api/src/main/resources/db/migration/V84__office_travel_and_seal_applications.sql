CREATE TABLE oa_travel_applications (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    code varchar(64) NOT NULL,
    applicant_id uuid NOT NULL REFERENCES sys_users(id),
    applicant_name varchar(80) NOT NULL,
    department_name varchar(120) NOT NULL,
    project_id uuid REFERENCES project_projects(id),
    destination varchar(160) NOT NULL,
    purpose varchar(800) NOT NULL,
    transport_type varchar(60) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    travel_days integer NOT NULL,
    estimated_amount numeric(14, 2) NOT NULL,
    companion_names varchar(500),
    status varchar(40) NOT NULL,
    approval_request_id uuid REFERENCES oa_approval_requests(id),
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT oa_travel_applications_tenant_code_key UNIQUE (tenant_id, code),
    CONSTRAINT oa_travel_applications_dates_check CHECK (end_date >= start_date),
    CONSTRAINT oa_travel_applications_days_check CHECK (travel_days > 0),
    CONSTRAINT oa_travel_applications_amount_check CHECK (estimated_amount >= 0)
);

CREATE INDEX idx_travel_status_dates ON oa_travel_applications (tenant_id, status, start_date);
CREATE INDEX idx_travel_applicant ON oa_travel_applications (tenant_id, applicant_id, created_at DESC);

CREATE TABLE oa_seal_applications (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    code varchar(64) NOT NULL,
    applicant_id uuid NOT NULL REFERENCES sys_users(id),
    applicant_name varchar(80) NOT NULL,
    department_name varchar(120) NOT NULL,
    seal_type varchar(60) NOT NULL,
    document_name varchar(240) NOT NULL,
    document_purpose varchar(800) NOT NULL,
    counterparty varchar(240),
    copy_count integer NOT NULL,
    use_date date NOT NULL,
    take_out boolean DEFAULT false NOT NULL,
    expected_return_date date,
    returned_at timestamptz,
    status varchar(40) NOT NULL,
    approval_request_id uuid REFERENCES oa_approval_requests(id),
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT oa_seal_applications_tenant_code_key UNIQUE (tenant_id, code),
    CONSTRAINT oa_seal_applications_copy_count_check CHECK (copy_count > 0),
    CONSTRAINT oa_seal_applications_return_date_check CHECK (
      (take_out = false AND expected_return_date IS NULL) OR
      (take_out = true AND expected_return_date IS NOT NULL AND expected_return_date >= use_date)
    )
);

CREATE INDEX idx_seal_status_date ON oa_seal_applications (tenant_id, status, use_date);
CREATE INDEX idx_seal_return ON oa_seal_applications (tenant_id, take_out, expected_return_date) WHERE returned_at IS NULL;

INSERT INTO sys_permissions (id, tenant_id, code, name, module, built_in, created_at, updated_at, version)
SELECT gen_random_uuid(), tenants.tenant_id, permission.code, permission.name, 'office', true, now(), now(), 0
FROM (SELECT DISTINCT tenant_id FROM sys_roles) tenants
CROSS JOIN (VALUES
  ('office:travel:view', '出差申请查看'),
  ('office:travel:create', '出差申请新增'),
  ('office:seal:view', '用印申请查看'),
  ('office:seal:create', '用印申请新增'),
  ('office:seal:return', '外带印章归还')
) AS permission(code, name)
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE permission.code IN ('office:travel:view', 'office:travel:create', 'office:seal:view', 'office:seal:create')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE role.code IN ('ADMIN', 'EXECUTIVE_MANAGER', 'HR_MANAGER', 'HR_SPECIALIST')
  AND permission.code = 'office:seal:return'
ON CONFLICT DO NOTHING;
