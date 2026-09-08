-- 工程汇报工时确认流程
-- 1) 工程汇报需填报工时（最小半日=4小时，全日=8小时），半日可选填写第二个项目（合计不超过8小时）
-- 2) 提交后生成审批单（业务类型 ENGINEERING_REPORT_CONFIRM），由项目经理确认
-- 3) 审批通过后工时计入 project_timesheets（已计入项目实际工时）
ALTER TABLE emp_reports
    ADD COLUMN IF NOT EXISTS hours numeric(5, 2),
    ADD COLUMN IF NOT EXISTS additional_project_id uuid,
    ADD COLUMN IF NOT EXISTS report_status varchar(16) NOT NULL DEFAULT 'PENDING_CONFIRM',
    ADD COLUMN IF NOT EXISTS approval_id uuid;

ALTER TABLE emp_reports
    ADD CONSTRAINT ck_emp_reports_status CHECK (report_status IN ('PENDING_CONFIRM', 'CONFIRMED', 'REJECTED'));

CREATE INDEX idx_emp_reports_status ON emp_reports (tenant_id, report_status);
CREATE INDEX idx_emp_reports_approval ON emp_reports (approval_id);

-- 工程汇报工时确认权限：项目经理确认入口
INSERT INTO sys_permissions (id, tenant_id, code, name, module, built_in, created_at, updated_at, version)
SELECT gen_random_uuid(), tenants.tenant_id, permission.code, permission.name, 'office', true, now(), now(), 0
FROM (SELECT DISTINCT tenant_id FROM sys_roles) tenants
CROSS JOIN (VALUES
    ('report:approve', '工程汇报工时确认')
) AS permission(code, name)
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 项目经理确认工程汇报工时（管理员保留全部审批权限）
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_roles role
JOIN sys_permissions permission ON permission.tenant_id = role.tenant_id
WHERE permission.code IN ('report:approve')
  AND role.code IN ('ADMIN', 'PROJECT_MANAGER', 'PROJECT_DIRECTOR')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permissions existing
      WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );

-- 工程汇报工时计入实际工时表时保留来源，防止重复计入
ALTER TABLE biz_project_timesheets
    ADD COLUMN IF NOT EXISTS source_report_id uuid,
    ADD COLUMN IF NOT EXISTS source_approval_id uuid;

CREATE INDEX idx_timesheet_source_report ON biz_project_timesheets (source_report_id);
