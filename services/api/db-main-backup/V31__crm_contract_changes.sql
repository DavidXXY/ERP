CREATE TABLE crm_contract_changes (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    contract_id UUID NOT NULL,
    change_data TEXT,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by VARCHAR(80) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_by VARCHAR(80),
    approved_at TIMESTAMP WITH TIME ZONE,
    approval_comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);
CREATE INDEX idx_crm_contract_changes_contract ON crm_contract_changes(contract_id);
CREATE INDEX idx_crm_contract_changes_status ON crm_contract_changes(status);