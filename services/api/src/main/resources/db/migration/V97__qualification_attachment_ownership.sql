CREATE TABLE qual_attachment_files (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  object_key VARCHAR(255) NOT NULL,
  owner_user_id UUID NOT NULL,
  original_name VARCHAR(240) NOT NULL,
  content_type VARCHAR(120),
  size_bytes BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_qual_attachment_tenant_object UNIQUE (tenant_id, object_key),
  CONSTRAINT fk_qual_attachment_owner FOREIGN KEY (owner_user_id) REFERENCES sys_users(id)
);

CREATE INDEX idx_qual_attachment_owner ON qual_attachment_files (tenant_id, owner_user_id);
