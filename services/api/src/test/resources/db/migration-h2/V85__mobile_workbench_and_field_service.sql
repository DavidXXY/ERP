ALTER TABLE work_orders ADD COLUMN assignment_accepted_at timestamp with time zone;
ALTER TABLE work_orders ADD COLUMN check_in_latitude numeric(10, 7);
ALTER TABLE work_orders ADD COLUMN check_in_longitude numeric(10, 7);
ALTER TABLE work_orders ADD COLUMN check_in_accuracy numeric(10, 2);
CREATE TABLE work_order_attachments (
    id uuid DEFAULT random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    work_order_id uuid NOT NULL,
    category varchar(40) NOT NULL,
    file_name varchar(240) NOT NULL,
    object_key varchar(500) NOT NULL,
    content_type varchar(120),
    file_size bigint NOT NULL,
    uploaded_by varchar(80),
    created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
    updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    FOREIGN KEY (work_order_id) REFERENCES work_orders(id) ON DELETE CASCADE
);
CREATE INDEX idx_work_order_attachments_order ON work_order_attachments (tenant_id, work_order_id, created_at);

CREATE TABLE work_order_mobile_operations (
    id uuid DEFAULT random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    work_order_id uuid NOT NULL,
    operation_id varchar(100) NOT NULL,
    operation_type varchar(40) NOT NULL,
    operated_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
    updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT work_order_mobile_operations_tenant_operation_key UNIQUE (tenant_id, operation_id),
    FOREIGN KEY (work_order_id) REFERENCES work_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (operated_by) REFERENCES sys_users(id)
);

CREATE TABLE sys_wechat_bindings (
    id uuid DEFAULT random_uuid() PRIMARY KEY,
    tenant_id varchar(64) DEFAULT 'default' NOT NULL,
    user_id uuid NOT NULL,
    app_id varchar(64) NOT NULL,
    open_id varchar(128) NOT NULL,
    union_id varchar(128),
    last_login_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
    updated_at timestamp with time zone DEFAULT current_timestamp NOT NULL,
    created_by varchar(64),
    updated_by varchar(64),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT sys_wechat_bindings_tenant_openid_key UNIQUE (tenant_id, app_id, open_id),
    CONSTRAINT sys_wechat_bindings_tenant_user_key UNIQUE (tenant_id, app_id, user_id),
    FOREIGN KEY (user_id) REFERENCES sys_users(id) ON DELETE CASCADE
);
