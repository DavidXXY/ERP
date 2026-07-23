CREATE TABLE IF NOT EXISTS biz_responsibility_bindings (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  source_type VARCHAR(40) NOT NULL,
  source_id UUID NOT NULL,
  owner_user_id UUID,
  department_id UUID,
  collaborator_department_ids VARCHAR(1000),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_responsibility_source UNIQUE (tenant_id, source_type, source_id)
);

CREATE TABLE IF NOT EXISTS biz_project_handovers (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  contract_id UUID NOT NULL,
  project_id UUID NOT NULL,
  sales_owner_id UUID,
  project_manager_id UUID,
  sales_department_id UUID,
  delivery_department_id UUID,
  scope_summary VARCHAR(1000),
  payment_terms VARCHAR(500),
  acceptance_criteria VARCHAR(1000),
  status VARCHAR(32) NOT NULL,
  submitted_at TIMESTAMP WITH TIME ZONE,
  accepted_at TIMESTAMP WITH TIME ZONE,
  accepted_by VARCHAR(80),
  comment VARCHAR(500),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_project_handover UNIQUE (tenant_id, project_id)
);

CREATE TABLE IF NOT EXISTS biz_project_staff_assignments (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  project_id UUID NOT NULL,
  user_id UUID NOT NULL,
  department_id UUID,
  role_name VARCHAR(80) NOT NULL,
  planned_hours NUMERIC(12,2) NOT NULL DEFAULT 0,
  actual_hours NUMERIC(12,2) NOT NULL DEFAULT 0,
  hourly_cost NUMERIC(12,2) NOT NULL DEFAULT 0,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  certificate_status VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_project_staff UNIQUE (tenant_id, project_id, user_id, role_name)
);

CREATE INDEX IF NOT EXISTS idx_responsibility_department ON biz_responsibility_bindings(tenant_id, department_id);
CREATE INDEX IF NOT EXISTS idx_handover_status ON biz_project_handovers(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_staff_project ON biz_project_staff_assignments(tenant_id, project_id);
