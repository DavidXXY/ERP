ALTER TABLE oa_approval_requests ADD COLUMN applicant_user_id uuid REFERENCES sys_users(id);

CREATE TABLE system_notification_reads (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    notification_id uuid NOT NULL REFERENCES system_notifications(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES sys_users(id) ON DELETE CASCADE,
    read_at timestamptz NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT system_notification_reads_tenant_message_user_key
      UNIQUE (tenant_id, notification_id, user_id)
);
CREATE INDEX idx_notification_reads_user ON system_notification_reads (tenant_id, user_id, read_at DESC);
CREATE INDEX idx_notifications_target_user ON system_notifications (tenant_id, target_user_id, created_at DESC);
CREATE INDEX idx_approval_applicant_user ON oa_approval_requests (tenant_id, applicant_user_id, created_at DESC);
CREATE INDEX idx_approval_delegated_user ON oa_approval_requests (tenant_id, delegated_user_id, created_at DESC);
CREATE INDEX idx_approval_actions_operator ON oa_approval_actions (tenant_id, operator_id, approval_id);
CREATE INDEX idx_approval_nodes_assignee
    ON oa_approval_runtime_nodes (tenant_id, node_status, assignee_type, assignee_id, approval_id);

INSERT INTO system_notification_reads (
    tenant_id, notification_id, user_id, read_at, created_at, updated_at
)
SELECT tenant_id, id, target_user_id, COALESCE(read_at, updated_at), now(), now()
FROM system_notifications
WHERE target_user_id IS NOT NULL AND is_read = true
ON CONFLICT DO NOTHING;

ALTER TABLE sys_users ADD COLUMN auth_version bigint DEFAULT 0 NOT NULL;
