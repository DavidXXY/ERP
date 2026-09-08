-- 员工汇报：区分日常汇报/工程汇报，工程汇报关联员工加入的项目
ALTER TABLE emp_reports
    ADD COLUMN IF NOT EXISTS report_category varchar(24) NOT NULL DEFAULT 'DAILY',
    ADD COLUMN IF NOT EXISTS project_id uuid;

ALTER TABLE emp_reports
    ADD CONSTRAINT ck_emp_reports_category CHECK (report_category IN ('DAILY', 'ENGINEERING'));

CREATE INDEX idx_emp_reports_project ON emp_reports (tenant_id, project_id, report_date DESC);
