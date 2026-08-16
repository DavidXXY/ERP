-- 流程规则责任人结构化：默认责任人/升级责任人由自由文本升级为
-- 角色(ROLE)/人员(USER)/岗位(POSITION)/动态(DYNAMIC) 四类引用。
-- default_owner / escalation_owner 继续作为“解析后的展示名称”保留。
ALTER TABLE public.risk_rule_configs
  ADD COLUMN default_owner_type character varying(20),
  ADD COLUMN default_owner_user_id uuid,
  ADD COLUMN default_owner_role_id uuid,
  ADD COLUMN default_owner_position character varying(120),
  ADD COLUMN default_owner_dynamic character varying(40),
  ADD COLUMN escalation_owner_type character varying(20),
  ADD COLUMN escalation_owner_user_id uuid,
  ADD COLUMN escalation_owner_role_id uuid,
  ADD COLUMN escalation_owner_position character varying(120),
  ADD COLUMN escalation_owner_dynamic character varying(40);
