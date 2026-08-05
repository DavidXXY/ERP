CREATE TABLE notification_channel_preferences (
  id uuid DEFAULT random_uuid() PRIMARY KEY, tenant_id varchar(64) DEFAULT 'default' NOT NULL,
  user_id uuid NOT NULL, channel varchar(32) NOT NULL, enabled boolean DEFAULT false NOT NULL,
  created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT uk_notification_preference UNIQUE (tenant_id, user_id, channel),
  FOREIGN KEY (user_id) REFERENCES sys_users(id)
);
CREATE TABLE notification_deliveries (
  id uuid DEFAULT random_uuid() PRIMARY KEY, tenant_id varchar(64) DEFAULT 'default' NOT NULL,
  notification_id uuid NOT NULL, user_id uuid NOT NULL, channel varchar(32) NOT NULL,
  status varchar(24) NOT NULL, attempt_count integer DEFAULT 0 NOT NULL,
  last_attempt_at timestamp with time zone, delivered_at timestamp with time zone, last_error varchar(500),
  created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
  created_by varchar(64), updated_by varchar(64), version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT uk_notification_delivery UNIQUE (tenant_id, notification_id, user_id, channel),
  FOREIGN KEY (notification_id) REFERENCES system_notifications(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES sys_users(id)
);
CREATE INDEX idx_notification_delivery_user ON notification_deliveries (tenant_id, user_id, created_at);
CREATE INDEX idx_system_notifications_dispatch ON system_notifications (tenant_id, created_at);
