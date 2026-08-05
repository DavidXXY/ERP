CREATE TABLE notification_channel_preferences (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  user_id uuid NOT NULL REFERENCES sys_users(id),
  channel varchar(32) NOT NULL,
  enabled boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by varchar(64), updated_by varchar(64), version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uk_notification_preference UNIQUE (tenant_id, user_id, channel)
);

CREATE TABLE notification_deliveries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id varchar(64) NOT NULL DEFAULT 'default',
  notification_id uuid NOT NULL REFERENCES system_notifications(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES sys_users(id),
  channel varchar(32) NOT NULL,
  status varchar(24) NOT NULL,
  attempt_count integer NOT NULL DEFAULT 0,
  last_attempt_at timestamptz,
  delivered_at timestamptz,
  last_error varchar(500),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by varchar(64), updated_by varchar(64), version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uk_notification_delivery UNIQUE (tenant_id, notification_id, user_id, channel)
);

CREATE INDEX idx_notification_delivery_user ON notification_deliveries (tenant_id, user_id, created_at DESC);
CREATE INDEX idx_system_notifications_dispatch ON system_notifications (tenant_id, created_at DESC);
