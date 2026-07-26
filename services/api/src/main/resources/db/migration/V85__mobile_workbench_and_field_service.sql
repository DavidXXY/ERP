ALTER TABLE work_orders
  ADD COLUMN assignment_accepted_at timestamptz,
  ADD COLUMN check_in_latitude numeric(10, 7),
  ADD COLUMN check_in_longitude numeric(10, 7),
  ADD COLUMN check_in_accuracy numeric(10, 2);

ALTER TABLE work_order_materials ALTER COLUMN part_id DROP NOT NULL;

CREATE TABLE work_order_attachments (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    work_order_id uuid NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    category varchar(40) NOT NULL,
    file_name varchar(240) NOT NULL,
    object_key varchar(500) NOT NULL,
    content_type varchar(120),
    file_size bigint NOT NULL,
    uploaded_by varchar(80),
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL
);
CREATE INDEX idx_work_order_attachments_order ON work_order_attachments (tenant_id, work_order_id, created_at);

CREATE TABLE work_order_mobile_operations (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    work_order_id uuid NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    operation_id varchar(100) NOT NULL,
    operation_type varchar(40) NOT NULL,
    operated_by uuid NOT NULL REFERENCES sys_users(id),
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT work_order_mobile_operations_tenant_operation_key UNIQUE (tenant_id, operation_id)
);
CREATE INDEX idx_work_order_mobile_operations_order ON work_order_mobile_operations (tenant_id, work_order_id, created_at);

CREATE TABLE sys_wechat_bindings (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    user_id uuid NOT NULL REFERENCES sys_users(id) ON DELETE CASCADE,
    app_id varchar(64) NOT NULL,
    open_id varchar(128) NOT NULL,
    union_id varchar(128),
    last_login_at timestamptz,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT sys_wechat_bindings_tenant_openid_key UNIQUE (tenant_id, app_id, open_id),
    CONSTRAINT sys_wechat_bindings_tenant_user_key UNIQUE (tenant_id, app_id, user_id)
);
CREATE INDEX idx_sys_wechat_bindings_user ON sys_wechat_bindings (tenant_id, user_id);
