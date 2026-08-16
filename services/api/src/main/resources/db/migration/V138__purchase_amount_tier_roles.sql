-- 将 PURCHASE 流程配置对齐为金额分级（恢复原采购分级审批模型）
-- 小额 [0, 1万)：采购专员；中额 [1万, 10万)：采购负责人；大额 [10万, ∞)：总经办负责人
-- 单一 SEQUENTIAL 流程，按金额命中唯一一条规则（单级审批），替代原先单一通用审批人角色

DELETE FROM approval_assignee_configs WHERE flow_code = 'PURCHASE';

INSERT INTO approval_assignee_configs
(id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version)
VALUES
('00000000-0000-4000-8000-000000000401', NOW(), NULL, 'default', NOW(), NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, TRUE, 'PURCHASE', '采购申请审批', 10000, 0, 100, NULL, '小额采购（1万以内）：采购专员审批', 1, NULL, NULL, 'ROLE', '0414bd4a-d51e-40a4-8396-21b0714b06b9', NULL, NULL, NULL, NULL, 1, 'PUBLISHED', NULL, 0),
('00000000-0000-4000-8000-000000000402', NOW(), NULL, 'default', NOW(), NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, TRUE, 'PURCHASE', '采购申请审批', 100000, 10000, 100, NULL, '中额采购（1万-10万）：采购负责人审批', 1, NULL, NULL, 'ROLE', '5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', NULL, NULL, NULL, NULL, 1, 'PUBLISHED', NULL, 0),
('00000000-0000-4000-8000-000000000403', NOW(), NULL, 'default', NOW(), NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, TRUE, 'PURCHASE', '采购申请审批', NULL, 100000, 100, NULL, '大额采购（10万以上）：总经办负责人审批', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 1, 'PUBLISHED', NULL, 0);
