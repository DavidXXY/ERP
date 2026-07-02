CREATE TABLE crm_attachment (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    attachment_type VARCHAR(20),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    uploaded_by VARCHAR(80) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_crm_attachment_entity ON crm_attachment(entity_type, entity_id);
