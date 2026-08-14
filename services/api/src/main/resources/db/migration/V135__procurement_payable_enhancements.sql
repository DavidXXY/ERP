-- 采购应付增强：应付冲减/作废、付款方式拆分、合并付款申请、发票容差与验真、多应付合并开票

-- 1. 应付单：累计冲减金额与作废信息
ALTER TABLE public.fin_procurement_payables
  ADD COLUMN IF NOT EXISTS adjusted_amount numeric(14, 2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS cancel_reason varchar(500),
  ADD COLUMN IF NOT EXISTS cancelled_by varchar(80),
  ADD COLUMN IF NOT EXISTS cancelled_at timestamp with time zone;

-- 2. 付款流水：直付（无申请）与付款方式拆分
ALTER TABLE public.fin_payment_records
  ADD COLUMN IF NOT EXISTS source_type varchar(20) NOT NULL DEFAULT 'APPLICATION',
  ADD COLUMN IF NOT EXISTS note varchar(500);
ALTER TABLE public.fin_payment_records ALTER COLUMN application_id DROP NOT NULL;

-- 3. 付款申请-应付 多对多（合并付款）
CREATE TABLE IF NOT EXISTS public.fin_payment_application_payables (
  id uuid DEFAULT gen_random_uuid() NOT NULL,
  tenant_id varchar(64) DEFAULT 'default'::character varying NOT NULL,
  application_id uuid NOT NULL REFERENCES public.fin_payment_applications(id),
  payable_id uuid NOT NULL REFERENCES public.fin_procurement_payables(id),
  allocated_amount numeric(14, 2) NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT fin_payment_application_payables_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_payment_app_payable
  ON public.fin_payment_application_payables (payable_id, application_id);
INSERT INTO public.fin_payment_application_payables
  (id, tenant_id, application_id, payable_id, allocated_amount, created_at, updated_at, version)
SELECT gen_random_uuid(), tenant_id, id, payable_id, requested_amount, created_at, updated_at, 0
FROM public.fin_payment_applications;

-- 4. 应付调整单（折让/索赔/更正/作废冲减）
CREATE TABLE IF NOT EXISTS public.fin_procurement_payable_adjustments (
  id uuid DEFAULT gen_random_uuid() NOT NULL,
  tenant_id varchar(64) DEFAULT 'default'::character varying NOT NULL,
  code varchar(64) NOT NULL,
  payable_id uuid NOT NULL REFERENCES public.fin_procurement_payables(id),
  order_id uuid NOT NULL REFERENCES public.procurement_purchase_orders(id),
  supplier_id uuid NOT NULL REFERENCES public.procurement_suppliers(id),
  adjustment_type varchar(32) NOT NULL,
  amount numeric(14, 2) NOT NULL,
  reason varchar(500),
  operator_name varchar(80) NOT NULL,
  applied_at date NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'APPLIED',
  source varchar(32) NOT NULL DEFAULT 'MANUAL',
  source_id uuid,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  created_by varchar(64),
  updated_by varchar(64),
  version bigint DEFAULT 0 NOT NULL,
  CONSTRAINT fin_procurement_payable_adjustments_pkey PRIMARY KEY (id),
  CONSTRAINT fin_procurement_payable_adjustments_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX IF NOT EXISTS idx_payable_adjustment_payable
  ON public.fin_procurement_payable_adjustments (payable_id, applied_at);

-- 5. 供应商发票验真字段
ALTER TABLE public.procurement_supplier_invoices
  ADD COLUMN IF NOT EXISTS verified_by varchar(80),
  ADD COLUMN IF NOT EXISTS verified_at timestamp with time zone,
  ADD COLUMN IF NOT EXISTS verification_comment varchar(500);

-- 6. 发票-应付 多对多（同一订单多次收货合并开票）
CREATE TABLE IF NOT EXISTS public.procurement_supplier_invoice_payables (
  id uuid DEFAULT gen_random_uuid() NOT NULL,
  tenant_id varchar(64) DEFAULT 'default'::character varying NOT NULL,
  invoice_id uuid NOT NULL REFERENCES public.procurement_supplier_invoices(id),
  payable_id uuid NOT NULL REFERENCES public.fin_procurement_payables(id),
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  CONSTRAINT procurement_supplier_invoice_payables_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_supplier_invoice_payable
  ON public.procurement_supplier_invoice_payables (invoice_id, payable_id);
INSERT INTO public.procurement_supplier_invoice_payables
  (id, tenant_id, invoice_id, payable_id, created_at, updated_at)
SELECT gen_random_uuid(), tenant_id, id, payable_id, created_at, updated_at
FROM public.procurement_supplier_invoices
WHERE payable_id IS NOT NULL;

-- 7. 供应商账期（默认 30 天，质检时未指定到期日则按此自动计算）
ALTER TABLE public.procurement_suppliers
  ADD COLUMN IF NOT EXISTS payment_terms_days integer NOT NULL DEFAULT 30;

-- 8. 索赔冲减过账科目（其他业务收入）
INSERT INTO public.fin_accounting_accounts
    (tenant_id, code, name, category, normal_direction, cash_account, active, system_account)
VALUES
    ('default', '6111', '其他业务收入', 'REVENUE', 'CREDIT', false, true, true)
ON CONFLICT (tenant_id, code) DO NOTHING;
