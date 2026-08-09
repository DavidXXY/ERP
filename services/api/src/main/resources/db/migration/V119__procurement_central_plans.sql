-- 集采计划：年度/周期集中采购计划，明细可一键转入采购申请
CREATE TABLE IF NOT EXISTS procurement_central_plans (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(180) NOT NULL,
  period_year INTEGER NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  remark VARCHAR(1000),
  created_by_name VARCHAR(80),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_proc_central_plans
  ON procurement_central_plans (tenant_id, period_year);

CREATE TABLE IF NOT EXISTS procurement_central_plan_items (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  plan_id UUID NOT NULL REFERENCES procurement_central_plans(id) ON DELETE CASCADE,
  part_id UUID NOT NULL,
  part_name VARCHAR(160) NOT NULL,
  planned_qty NUMERIC(14,2) NOT NULL,
  unit_price NUMERIC(14,2) NOT NULL DEFAULT 0,
  expected_date DATE,
  request_id UUID,
  status VARCHAR(24) NOT NULL DEFAULT 'PLANNED',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_proc_central_plan_items_plan
  ON procurement_central_plan_items (tenant_id, plan_id);
