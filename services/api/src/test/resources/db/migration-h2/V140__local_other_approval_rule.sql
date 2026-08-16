insert into approval_assignee_configs (
  id,
  tenant_id,
  created_at,
  updated_at,
  flow_code,
  flow_name,
  user_id,
  approval_mode,
  sequence_no,
  enabled,
  assignee_type,
  role_id,
  condition_type,
  priority,
  version_no,
  dynamic_assignee,
  step_policy,
  publish_status
)
select
  '00000000-0000-4000-8000-000000000141',
  'default',
  current_timestamp,
  current_timestamp,
  'OTHER',
  '通用审批',
  null,
  'SEQUENTIAL',
  1,
  true,
  'ROLE',
  '00000000-0000-4000-8000-000000000101',
  'ANY',
  200,
  1,
  null,
  'ANY_APPROVE',
  'PUBLISHED'
where not exists (
  select 1
  from approval_assignee_configs
  where tenant_id = 'default' and flow_code = 'OTHER'
);
