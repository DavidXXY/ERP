-- Flyway baseline migration for fresh PostgreSQL 16 installations.
-- This snapshot replaces the historical V1-V77 chain. It contains the complete
-- schema and required organization/role/permission/approval configuration only.
-- It intentionally contains no users, credentials, test data, or business data.
-- Incremental migrations start at V78 so existing V77 installations and fresh
-- installations created from this B77 snapshot follow the same upgrade path.

--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: approval_assignee_configs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.approval_assignee_configs (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    approval_mode character varying(20) NOT NULL,
    business_type character varying(80),
    condition_type character varying(32) NOT NULL,
    customer_level character varying(40),
    department_name character varying(120),
    enabled boolean NOT NULL,
    flow_code character varying(64) NOT NULL,
    flow_name character varying(120) NOT NULL,
    max_amount numeric(18,2),
    min_amount numeric(18,2),
    priority integer NOT NULL,
    project_code character varying(80),
    remark character varying(500),
    sequence_no integer NOT NULL,
    supplier_risk character varying(40),
    user_id uuid,
    assignee_type character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    role_id uuid,
    auto_action character varying(20),
    dynamic_assignee character varying(40),
    escalation_role_id uuid,
    sla_hours integer,
    version_no integer DEFAULT 1 NOT NULL,
    publish_status character varying(20),
    step_policy character varying(32),
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_approval_assignee_target CHECK (((((assignee_type)::text = 'USER'::text) AND (user_id IS NOT NULL) AND (role_id IS NULL) AND (dynamic_assignee IS NULL)) OR (((assignee_type)::text = 'ROLE'::text) AND (role_id IS NOT NULL) AND (user_id IS NULL) AND (dynamic_assignee IS NULL)) OR (((assignee_type)::text = 'DYNAMIC'::text) AND (dynamic_assignee IS NOT NULL) AND (user_id IS NULL) AND (role_id IS NULL))))
);


--
-- Name: biz_collaboration_action_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_collaboration_action_logs (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    source_type character varying(40) NOT NULL,
    source_id uuid NOT NULL,
    action_type character varying(40) NOT NULL,
    operator_user_id uuid,
    operator_name character varying(80) NOT NULL,
    target_user_id uuid,
    comment character varying(1000),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: biz_collaboration_task_controls; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_collaboration_task_controls (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    source_type character varying(40) NOT NULL,
    source_id uuid NOT NULL,
    assignee_user_id uuid,
    cc_user_ids character varying(2000),
    status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    due_at timestamp with time zone,
    completed_at timestamp with time zone,
    last_comment character varying(1000),
    reminder_count integer DEFAULT 0 NOT NULL,
    last_reminded_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: biz_project_budget_versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_project_budget_versions (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    project_id uuid NOT NULL,
    version_no integer NOT NULL,
    previous_amount numeric(14,2) DEFAULT 0 NOT NULL,
    requested_amount numeric(14,2) NOT NULL,
    status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    reason character varying(1000) NOT NULL,
    requested_by uuid,
    requested_by_name character varying(80) NOT NULL,
    reviewed_by uuid,
    reviewed_by_name character varying(80),
    review_comment character varying(500),
    reviewed_at timestamp with time zone,
    effective_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: biz_project_handovers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_project_handovers (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    contract_id uuid NOT NULL,
    project_id uuid NOT NULL,
    sales_owner_id uuid,
    project_manager_id uuid,
    sales_department_id uuid,
    delivery_department_id uuid,
    scope_summary character varying(1000),
    payment_terms character varying(500),
    acceptance_criteria character varying(1000),
    status character varying(32) NOT NULL,
    submitted_at timestamp with time zone,
    accepted_at timestamp with time zone,
    accepted_by character varying(80),
    comment character varying(500),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL,
    customer_contact character varying(500),
    technical_solution character varying(1000),
    quotation_summary character varying(1000),
    risk_notes character varying(1000)
);


--
-- Name: biz_project_staff_assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_project_staff_assignments (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    project_id uuid NOT NULL,
    user_id uuid NOT NULL,
    department_id uuid,
    role_name character varying(80) NOT NULL,
    planned_hours numeric(12,2) DEFAULT 0 NOT NULL,
    actual_hours numeric(12,2) DEFAULT 0 NOT NULL,
    hourly_cost numeric(12,2) DEFAULT 0 NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    certificate_status character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL,
    allocation_percent numeric(5,2) DEFAULT 100 NOT NULL
);


--
-- Name: biz_project_timesheets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_project_timesheets (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    assignment_id uuid NOT NULL,
    project_id uuid NOT NULL,
    user_id uuid NOT NULL,
    work_date date NOT NULL,
    hours numeric(8,2) NOT NULL,
    description character varying(500),
    status character varying(32) DEFAULT 'SUBMITTED'::character varying NOT NULL,
    submitted_by uuid,
    reviewed_by uuid,
    reviewed_by_name character varying(80),
    review_comment character varying(500),
    reviewed_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: biz_responsibility_bindings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_responsibility_bindings (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    source_type character varying(40) NOT NULL,
    source_id uuid NOT NULL,
    owner_user_id uuid,
    department_id uuid,
    collaborator_department_ids character varying(1000),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: biz_responsibility_collaborators; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_responsibility_collaborators (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    binding_id uuid NOT NULL,
    department_id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: biz_timesheet_period_locks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_timesheet_period_locks (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    year_month character varying(7) NOT NULL,
    locked_by uuid,
    locked_by_name character varying(80) NOT NULL,
    locked_at timestamp with time zone NOT NULL,
    reason character varying(500),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: code_sequences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.code_sequences (
    entity_type character varying(64) NOT NULL,
    prefix character varying(16) NOT NULL,
    next_number bigint DEFAULT 1 NOT NULL
);


--
-- Name: crm_attachment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_attachment (
    id uuid NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id uuid NOT NULL,
    attachment_type character varying(20),
    file_name character varying(255) NOT NULL,
    file_path character varying(500) NOT NULL,
    file_size bigint NOT NULL,
    mime_type character varying(100),
    uploaded_by character varying(80) NOT NULL,
    uploaded_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: crm_contract_changes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_contract_changes (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    approval_comment text,
    approved_at timestamp(6) with time zone,
    approved_by character varying(80),
    change_data text,
    contract_id uuid NOT NULL,
    reason character varying(500),
    requested_at timestamp(6) with time zone NOT NULL,
    requested_by character varying(80) NOT NULL,
    status character varying(20) NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: crm_customer_contacts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_customer_contacts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    customer_id uuid NOT NULL,
    name character varying(80) NOT NULL,
    title character varying(80),
    phone character varying(40),
    email character varying(120),
    primary_contact boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: crm_customer_sites; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_customer_sites (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    customer_id uuid NOT NULL,
    name character varying(120) NOT NULL,
    address character varying(240) NOT NULL,
    longitude numeric(10,6),
    latitude numeric(10,6),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: crm_customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_customers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(160) NOT NULL,
    industry character varying(80) NOT NULL,
    level character varying(32) NOT NULL,
    owner_name character varying(80) NOT NULL,
    payment_habit character varying(200),
    risk_status character varying(32) DEFAULT 'NORMAL'::character varying NOT NULL,
    risk_note character varying(500),
    invoice_title character varying(180),
    tax_no character varying(512),
    bank_name character varying(160),
    bank_account character varying(512),
    registered_address character varying(240),
    registered_phone character varying(40),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: crm_follow_ups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_follow_ups (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    customer_id uuid NOT NULL,
    opportunity_id uuid,
    type character varying(32) NOT NULL,
    subject character varying(160) NOT NULL,
    content character varying(1200) NOT NULL,
    followed_at timestamp with time zone NOT NULL,
    next_action character varying(240),
    owner_name character varying(80) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: crm_opportunities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_opportunities (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    customer_id uuid,
    code character varying(64) NOT NULL,
    source character varying(80),
    need_summary character varying(500) NOT NULL,
    stage character varying(40) NOT NULL,
    expected_amount numeric(14,2) DEFAULT 0 NOT NULL,
    probability integer DEFAULT 0 NOT NULL,
    next_action character varying(240),
    next_action_at date,
    owner_name character varying(80),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: crm_quote_approval_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_quote_approval_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    quote_id uuid NOT NULL,
    decision character varying(32) NOT NULL,
    comment character varying(500) NOT NULL,
    approver_name character varying(80) NOT NULL,
    decided_at timestamp with time zone NOT NULL,
    generated_contract_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    quote_version integer DEFAULT 1 NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: crm_quote_cost_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_quote_cost_requests (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    approval_comment character varying(500),
    approved_at timestamp(6) with time zone,
    approved_by character varying(80),
    cost_remark character varying(800),
    customer_id uuid,
    equipment_cost numeric(14,2) NOT NULL,
    labor_cost numeric(14,2) NOT NULL,
    material_cost numeric(14,2) NOT NULL,
    opportunity_id uuid,
    other_cost numeric(14,2) NOT NULL,
    project_manager character varying(80),
    quote_id uuid NOT NULL,
    requested_at timestamp(6) with time zone NOT NULL,
    requested_by character varying(80) NOT NULL,
    risk_reserve numeric(14,2) NOT NULL,
    status character varying(32) NOT NULL,
    subcontract_cost numeric(14,2) NOT NULL,
    submitted_at timestamp(6) with time zone,
    suggested_price numeric(14,2),
    travel_cost numeric(14,2) NOT NULL,
    labor_tax_rate numeric(5,2) DEFAULT 6 NOT NULL,
    material_tax_rate numeric(5,2) DEFAULT 13 NOT NULL,
    subcontract_tax_rate numeric(5,2) DEFAULT 13 NOT NULL,
    travel_tax_rate numeric(5,2) DEFAULT 0 NOT NULL,
    equipment_tax_rate numeric(5,2) DEFAULT 13 NOT NULL,
    risk_reserve_tax_rate numeric(5,2) DEFAULT 0 NOT NULL,
    other_tax_rate numeric(5,2) DEFAULT 13 NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT crm_quote_cost_requests_status_check CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'SUBMITTED'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: crm_quote_plans; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_quote_plans (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    customer_id uuid,
    opportunity_id uuid,
    code character varying(64) NOT NULL,
    service_scope character varying(800) NOT NULL,
    inspect_cycle character varying(120),
    payment_nodes character varying(300),
    amount numeric(14,2) DEFAULT 0 NOT NULL,
    status character varying(40) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version_no integer DEFAULT 1 NOT NULL,
    customer_decision character varying(32),
    customer_comment character varying(500),
    customer_decision_by character varying(80),
    customer_decided_at timestamp with time zone,
    labor_budget numeric(14,2) NOT NULL,
    material_budget numeric(14,2) NOT NULL,
    other_budget numeric(14,2) NOT NULL,
    subcontract_budget numeric(14,2) NOT NULL,
    travel_budget numeric(14,2) NOT NULL,
    tax_rate numeric(5,2) NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: crm_quote_revisions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_quote_revisions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    quote_id uuid NOT NULL,
    version_no integer NOT NULL,
    code character varying(64) NOT NULL,
    service_scope character varying(800) NOT NULL,
    inspect_cycle character varying(120),
    payment_nodes character varying(300),
    amount numeric(14,2) NOT NULL,
    status character varying(40) NOT NULL,
    revision_note character varying(500) NOT NULL,
    editor_name character varying(80) NOT NULL,
    revised_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    labor_budget numeric(14,2) NOT NULL,
    material_budget numeric(14,2) NOT NULL,
    other_budget numeric(14,2) NOT NULL,
    subcontract_budget numeric(14,2) NOT NULL,
    travel_budget numeric(14,2) NOT NULL,
    tax_rate numeric(5,2) NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: crm_service_contracts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crm_service_contracts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    customer_id uuid NOT NULL,
    code character varying(64) NOT NULL,
    project_name character varying(160) NOT NULL,
    contract_type character varying(80) NOT NULL,
    amount numeric(14,2) DEFAULT 0 NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    service_cycle character varying(120),
    status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    quote_id uuid,
    tax_rate numeric(5,2) NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: doc_files; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.doc_files (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    biz_type character varying(80) NOT NULL,
    biz_id uuid,
    file_name character varying(240) NOT NULL,
    object_key character varying(500) NOT NULL,
    content_type character varying(120),
    size_bytes bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: fin_accounting_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fin_accounting_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    voucher_id uuid NOT NULL,
    account_code character varying(32) NOT NULL,
    account_name character varying(120) NOT NULL,
    debit numeric(14,2) DEFAULT 0 NOT NULL,
    credit numeric(14,2) DEFAULT 0 NOT NULL,
    summary character varying(300),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: fin_accounting_vouchers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fin_accounting_vouchers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    biz_type character varying(60) NOT NULL,
    biz_no character varying(80) NOT NULL,
    voucher_date date NOT NULL,
    description character varying(500) NOT NULL,
    status character varying(32) NOT NULL,
    total_debit numeric(14,2) NOT NULL,
    total_credit numeric(14,2) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: fin_payment_applications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fin_payment_applications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    payable_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    requested_amount numeric(14,2) NOT NULL,
    requested_date date NOT NULL,
    applicant_name character varying(80) NOT NULL,
    purpose character varying(300) NOT NULL,
    status character varying(32) DEFAULT 'PENDING_APPROVAL'::character varying NOT NULL,
    approval_comment character varying(500),
    approver_name character varying(80),
    approved_at timestamp with time zone,
    payment_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: fin_payment_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fin_payment_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    application_id uuid NOT NULL,
    payable_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    amount numeric(14,2) NOT NULL,
    paid_date date NOT NULL,
    payment_method character varying(32) NOT NULL,
    bank_reference character varying(100) NOT NULL,
    payer_name character varying(80) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: fin_procurement_payables; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fin_procurement_payables (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    supplier_id uuid NOT NULL,
    order_id uuid NOT NULL,
    receipt_id uuid NOT NULL,
    amount numeric(14,2) NOT NULL,
    paid_amount numeric(14,2) DEFAULT 0 NOT NULL,
    due_date date NOT NULL,
    status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    tax_rate numeric(5,2) NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: fin_receivable_receipts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fin_receivable_receipts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    receivable_id uuid NOT NULL,
    amount numeric(14,2) NOT NULL,
    received_date date NOT NULL,
    reference_no character varying(80) NOT NULL,
    recorder_name character varying(80) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: fin_receivables; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fin_receivables (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    customer_id uuid NOT NULL,
    contract_id uuid,
    code character varying(64) NOT NULL,
    source_no character varying(64) NOT NULL,
    amount numeric(14,2) DEFAULT 0 NOT NULL,
    due_date date NOT NULL,
    status character varying(32) DEFAULT 'INVOICE_PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    invoice_no character varying(80),
    invoice_date date,
    settled_amount numeric(14,2) DEFAULT 0 NOT NULL,
    invoice_request_remark character varying(500),
    invoice_requested_at timestamp(6) with time zone,
    invoice_requested_by character varying(80),
    invoice_requested boolean DEFAULT false NOT NULL,
    invoice_review_comment character varying(500),
    invoice_reviewed_at timestamp(6) with time zone,
    invoice_reviewed_by character varying(80),
    version bigint DEFAULT 0 NOT NULL,
    invoice_request_status character varying(32) DEFAULT 'NOT_REQUESTED'::character varying NOT NULL
);


--
-- Name: hr_emergency_contacts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_emergency_contacts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    employee_id uuid,
    name character varying(80) NOT NULL,
    relationship character varying(40),
    phone character varying(40),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_by character varying(64),
    address character varying(200),
    is_primary boolean NOT NULL,
    remark character varying(1000),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: hr_employee_certificates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_employee_certificates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    user_id uuid NOT NULL,
    certificate_type character varying(120) NOT NULL,
    certificate_no character varying(120) NOT NULL,
    issue_date date,
    expiry_date date NOT NULL,
    issuing_authority character varying(180),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: hr_employee_contracts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_employee_contracts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    employee_id uuid NOT NULL,
    contract_no character varying(100) NOT NULL,
    contract_type character varying(80) NOT NULL,
    sign_date date,
    start_date date NOT NULL,
    end_date date,
    probation_end_date date,
    status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    attachments_json character varying(4000),
    remark character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: hr_employee_education; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_employee_education (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    degree character varying(40),
    end_date date,
    is_highest boolean NOT NULL,
    major character varying(200),
    remark character varying(1000),
    school_name character varying(200) NOT NULL,
    start_date date,
    employee_id uuid NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: hr_employee_lifecycle_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_employee_lifecycle_records (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    approved_at date,
    approved_by character varying(80),
    effective_date date NOT NULL,
    from_organization_name character varying(200),
    from_organization_id character varying(64),
    from_position character varying(120),
    lifecycle_type character varying(40) NOT NULL,
    reason character varying(1000),
    remark character varying(1000),
    status character varying(32) NOT NULL,
    to_organization_name character varying(200),
    to_organization_id character varying(64),
    to_position character varying(120),
    employee_id uuid NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: hr_employee_work_experience; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_employee_work_experience (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    company_name character varying(200) NOT NULL,
    is_current boolean NOT NULL,
    description character varying(1000),
    end_date date,
    position_name character varying(120),
    remark character varying(1000),
    start_date date,
    employee_id uuid NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: hr_field_attendance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_field_attendance (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    user_id uuid NOT NULL,
    work_order_id uuid NOT NULL,
    check_in_at timestamp with time zone NOT NULL,
    check_out_at timestamp with time zone,
    check_in_location character varying(300) NOT NULL,
    check_out_location character varying(300),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: hr_field_schedules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_field_schedules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    user_id uuid NOT NULL,
    work_date date NOT NULL,
    shift_name character varying(80) NOT NULL,
    site_name character varying(180),
    status character varying(32) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: hr_leave_balances; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_leave_balances (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    leave_type character varying(40) NOT NULL,
    total_days double precision NOT NULL,
    used_days double precision NOT NULL,
    employee_id uuid NOT NULL,
    leave_year integer NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: hr_leave_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.hr_leave_requests (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    approval_remark character varying(1000),
    approved_at timestamp(6) without time zone,
    approved_by character varying(80),
    end_date date NOT NULL,
    leave_type character varying(40) NOT NULL,
    reason character varying(1000),
    start_date date NOT NULL,
    status character varying(32) NOT NULL,
    total_days double precision NOT NULL,
    employee_id uuid NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: inventory_issue_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventory_issue_lines (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    issue_id uuid NOT NULL,
    part_id uuid NOT NULL,
    part_name character varying(160) NOT NULL,
    quantity numeric(14,2) NOT NULL,
    returned_qty numeric(14,2) DEFAULT 0 NOT NULL,
    unit_cost numeric(14,2) NOT NULL,
    amount numeric(14,2) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: inventory_issue_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventory_issue_orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    project_id uuid NOT NULL,
    issue_date date NOT NULL,
    receiver_name character varying(80) NOT NULL,
    purpose character varying(300) NOT NULL,
    total_amount numeric(14,2) DEFAULT 0 NOT NULL,
    status character varying(32) DEFAULT 'POSTED'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: inventory_parts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventory_parts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(160) NOT NULL,
    model character varying(120),
    stock_qty numeric(14,2) DEFAULT 0 NOT NULL,
    safety_qty numeric(14,2) DEFAULT 0 NOT NULL,
    location character varying(80),
    unit_cost numeric(14,2) DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: inventory_return_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventory_return_lines (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    return_id uuid NOT NULL,
    issue_line_id uuid NOT NULL,
    part_id uuid NOT NULL,
    part_name character varying(160) NOT NULL,
    quantity numeric(14,2) NOT NULL,
    unit_cost numeric(14,2) NOT NULL,
    amount numeric(14,2) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: inventory_return_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventory_return_orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    issue_id uuid NOT NULL,
    project_id uuid NOT NULL,
    return_date date NOT NULL,
    handler_name character varying(80) NOT NULL,
    total_amount numeric(14,2) DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: inventory_stock_movements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventory_stock_movements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    part_id uuid NOT NULL,
    movement_type character varying(40) NOT NULL,
    quantity numeric(14,2) NOT NULL,
    source_no character varying(64),
    remark character varying(300),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: maintenance_equipment_assets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.maintenance_equipment_assets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    customer_id uuid NOT NULL,
    contract_id uuid,
    code character varying(64) NOT NULL,
    name character varying(160) NOT NULL,
    category character varying(80) NOT NULL,
    model character varying(120),
    serial_no character varying(120),
    site_address character varying(300) NOT NULL,
    installed_date date,
    warranty_end_date date,
    maintenance_cycle_days integer DEFAULT 90 NOT NULL,
    last_maintenance_date date,
    next_maintenance_date date,
    status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    required_certificate character varying(120),
    notes character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: maintenance_plans; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.maintenance_plans (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    asset_id uuid NOT NULL,
    contract_id uuid,
    plan_name character varying(180) NOT NULL,
    cycle_days integer NOT NULL,
    next_due_date date NOT NULL,
    last_generated_date date,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: oa_approval_actions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oa_approval_actions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    approval_id uuid NOT NULL,
    decision character varying(40) NOT NULL,
    operator_name character varying(80) NOT NULL,
    comment character varying(500) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    action_type character varying(32) NOT NULL,
    operator_id uuid,
    step_no integer,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: oa_approval_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oa_approval_requests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    approval_type character varying(80) NOT NULL,
    title character varying(180) NOT NULL,
    source_no character varying(64),
    amount numeric(14,2),
    status character varying(40) DEFAULT 'PENDING'::character varying NOT NULL,
    applicant_name character varying(80),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    content character varying(1000),
    approver_name character varying(80),
    approval_comment character varying(500),
    processed_at timestamp with time zone,
    approval_mode character varying(20),
    business_type character varying(80),
    current_approver_name character varying(120),
    current_step integer,
    customer_level character varying(40),
    delegated_user_id uuid,
    department_name character varying(120),
    matched_rule_text character varying(500),
    project_code character varying(80),
    supplier_risk character varying(40),
    total_steps integer,
    approval_config_version integer,
    approval_plan_snapshot text,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: oa_approval_runtime_nodes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oa_approval_runtime_nodes (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    approval_comment character varying(500),
    approval_id uuid NOT NULL,
    approval_mode character varying(20),
    approver_id uuid,
    approver_name character varying(80),
    assignee_id uuid,
    assignee_name character varying(120),
    assignee_type character varying(20),
    completed_at timestamp(6) with time zone,
    condition_text character varying(500),
    due_at timestamp(6) with time zone,
    escalated_at timestamp(6) with time zone,
    escalation_role_id uuid,
    node_status character varying(32) NOT NULL,
    reminded_at timestamp(6) with time zone,
    sla_hours integer,
    source_type character varying(40),
    source_value character varying(120),
    step_no integer NOT NULL,
    step_policy character varying(32),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: oa_expense_claim_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oa_expense_claim_lines (
    id uuid NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    created_by character varying(64),
    updated_by character varying(64),
    expense_id uuid NOT NULL,
    line_no integer NOT NULL,
    expense_type character varying(40) NOT NULL,
    amount numeric(14,2) NOT NULL,
    expense_date date NOT NULL,
    description character varying(500) NOT NULL,
    invoice_file_name character varying(240),
    invoice_content_type character varying(120),
    invoice_size_bytes bigint,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: oa_expense_claims; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oa_expense_claims (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    claimant_id uuid,
    claimant_name character varying(80) NOT NULL,
    project_id uuid,
    work_order_id uuid,
    expense_type character varying(40) NOT NULL,
    amount numeric(14,2) NOT NULL,
    expense_date date NOT NULL,
    description character varying(500) NOT NULL,
    status character varying(40) NOT NULL,
    approval_request_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: oa_outsource_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oa_outsource_orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    supplier_id uuid NOT NULL,
    project_id uuid,
    work_order_id uuid,
    service_type character varying(100) NOT NULL,
    description character varying(800) NOT NULL,
    amount numeric(14,2) NOT NULL,
    planned_date date NOT NULL,
    status character varying(40) NOT NULL,
    approval_request_id uuid,
    acceptance_note character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: operation_audits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.operation_audits (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    username character varying(80),
    http_method character varying(12) NOT NULL,
    request_path character varying(500) NOT NULL,
    response_status integer NOT NULL,
    client_ip character varying(80),
    duration_ms bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: procurement_action_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_action_logs (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    source_type character varying(40) NOT NULL,
    source_id uuid NOT NULL,
    action_type character varying(40) NOT NULL,
    operator_name character varying(80) NOT NULL,
    details character varying(2000),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: procurement_collaboration_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_collaboration_events (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    supplier_id uuid NOT NULL,
    order_id uuid,
    event_type character varying(40) NOT NULL,
    reference_no character varying(100),
    event_date date NOT NULL,
    promised_date date,
    quantity numeric(14,2),
    status character varying(32) DEFAULT 'OPEN'::character varying NOT NULL,
    content character varying(1000),
    created_by_name character varying(80) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: procurement_contracts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_contracts (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    contract_no character varying(80) NOT NULL,
    name character varying(180) NOT NULL,
    supplier_id uuid NOT NULL,
    amount numeric(14,2) NOT NULL,
    currency character varying(8) DEFAULT 'CNY'::character varying NOT NULL,
    start_date date,
    end_date date,
    payment_terms character varying(500),
    status character varying(32) DEFAULT 'DRAFT'::character varying NOT NULL,
    approval_status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    version_no integer DEFAULT 1 NOT NULL,
    parent_contract_id uuid,
    change_reason character varying(1000),
    submitted_by_name character varying(80),
    submitted_at timestamp with time zone,
    approved_by_name character varying(80),
    approval_comment character varying(500),
    approved_at timestamp with time zone,
    remark character varying(1000),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: procurement_cost_allocations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_cost_allocations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    order_id uuid NOT NULL,
    receipt_id uuid NOT NULL,
    cost_type character varying(24) NOT NULL,
    project_id uuid,
    department_id uuid,
    target_code character varying(64) NOT NULL,
    target_name character varying(180) NOT NULL,
    part_name character varying(160) NOT NULL,
    amount numeric(14,2) NOT NULL,
    incurred_date date NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: procurement_goods_receipts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_goods_receipts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    order_id uuid NOT NULL,
    part_id uuid NOT NULL,
    quantity numeric(14,2) NOT NULL,
    unit_price numeric(14,2) NOT NULL,
    amount numeric(14,2) NOT NULL,
    received_date date NOT NULL,
    delivery_no character varying(80) NOT NULL,
    receiver_name character varying(80) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    tax_rate numeric(5,2) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    inspection_status character varying(32) DEFAULT 'PASSED'::character varying NOT NULL,
    qualified_qty numeric(14,2),
    rejected_qty numeric(14,2) DEFAULT 0 NOT NULL,
    inspector_name character varying(80),
    inspection_comment character varying(500),
    inspected_at timestamp with time zone,
    payable_due_date date,
    client_request_id character varying(80),
    asn_no character varying(80)
);


--
-- Name: procurement_inquiries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_inquiries (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    request_id uuid NOT NULL,
    code character varying(64) NOT NULL,
    title character varying(180) NOT NULL,
    deadline date,
    status character varying(32) NOT NULL,
    created_by_name character varying(80),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL,
    sourcing_method character varying(32) DEFAULT 'COMPETITIVE'::character varying NOT NULL,
    min_quote_count integer DEFAULT 3 NOT NULL,
    exception_reason character varying(500),
    selected_quote_id uuid,
    selection_reason character varying(1000),
    selected_by_name character varying(80),
    selected_at timestamp with time zone
);


--
-- Name: procurement_inquiry_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_inquiry_requests (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    inquiry_id uuid NOT NULL,
    request_id uuid NOT NULL,
    requested_qty numeric(14,2) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: procurement_purchase_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_purchase_orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    supplier_id uuid NOT NULL,
    request_id uuid,
    order_amount numeric(14,2) DEFAULT 0 NOT NULL,
    expected_delivery_date date,
    status character varying(32) DEFAULT 'ORDERED'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    part_id uuid,
    part_name character varying(160),
    ordered_qty numeric(14,2) DEFAULT 0 NOT NULL,
    received_qty numeric(14,2) DEFAULT 0 NOT NULL,
    unit_price numeric(14,2) DEFAULT 0 NOT NULL,
    cost_type character varying(24) DEFAULT 'DEPARTMENT'::character varying NOT NULL,
    project_id uuid,
    department_id uuid,
    cost_target_code character varying(64) DEFAULT 'PROCUREMENT_DEPARTMENT'::character varying NOT NULL,
    cost_target_name character varying(180) DEFAULT '采购部'::character varying NOT NULL,
    tax_rate numeric(5,2) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    approval_status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    approval_comment character varying(500),
    approver_name character varying(80),
    approved_at timestamp with time zone,
    inquiry_id uuid,
    contract_id uuid,
    currency character varying(8) DEFAULT 'CNY'::character varying NOT NULL,
    freight_amount numeric(14,2) DEFAULT 0 NOT NULL,
    source_reason character varying(1000),
    submitted_at timestamp with time zone,
    closed_at timestamp with time zone,
    order_version integer DEFAULT 1 NOT NULL
);


--
-- Name: procurement_purchase_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_purchase_requests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    requester_name character varying(80) NOT NULL,
    part_id uuid,
    part_name character varying(160) NOT NULL,
    quantity numeric(14,2) DEFAULT 0 NOT NULL,
    expected_date date,
    reason character varying(300),
    status character varying(32) DEFAULT 'SUBMITTED'::character varying NOT NULL,
    approval_status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    cost_type character varying(24) DEFAULT 'DEPARTMENT'::character varying NOT NULL,
    project_id uuid,
    department_id uuid,
    cost_target_code character varying(64) DEFAULT 'PROCUREMENT_DEPARTMENT'::character varying NOT NULL,
    cost_target_name character varying(180) DEFAULT '采购部'::character varying NOT NULL,
    tax_rate numeric(5,2) NOT NULL,
    total_amount numeric(14,2) NOT NULL,
    unit_price numeric(14,2) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    source_type character varying(32) DEFAULT 'MANUAL'::character varying NOT NULL,
    source_reference character varying(120),
    batch_id uuid NOT NULL,
    batch_code character varying(64) NOT NULL,
    batch_name character varying(180),
    line_no integer NOT NULL
);


--
-- Name: procurement_request_approval_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_request_approval_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    request_id uuid NOT NULL,
    decision character varying(32) NOT NULL,
    comment character varying(500) NOT NULL,
    approver_name character varying(80) NOT NULL,
    decided_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: procurement_return_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_return_orders (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    code character varying(64) NOT NULL,
    order_id uuid NOT NULL,
    receipt_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    quantity numeric(14,2) NOT NULL,
    amount numeric(14,2) NOT NULL,
    reason character varying(500) NOT NULL,
    return_date date NOT NULL,
    handler_name character varying(80) NOT NULL,
    status character varying(32) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL,
    replacement_qty numeric(14,2) DEFAULT 0 NOT NULL,
    credit_amount numeric(14,2) DEFAULT 0 NOT NULL,
    claim_amount numeric(14,2) DEFAULT 0 NOT NULL,
    corrective_action character varying(1000),
    supplier_response character varying(1000),
    completed_at timestamp with time zone
);


--
-- Name: procurement_supplier_change_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_supplier_change_requests (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    supplier_id uuid NOT NULL,
    change_type character varying(40) NOT NULL,
    proposed_admission_status character varying(40),
    proposed_risk_status character varying(32),
    proposed_bank_name character varying(120),
    proposed_bank_account character varying(512),
    proposed_settlement_terms character varying(160),
    reason character varying(1000) NOT NULL,
    status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    requested_by_name character varying(80) NOT NULL,
    reviewed_by_name character varying(80),
    review_comment character varying(500),
    reviewed_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: procurement_supplier_invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_supplier_invoices (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    code character varying(64) NOT NULL,
    invoice_no character varying(100) NOT NULL,
    order_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    amount numeric(14,2) NOT NULL,
    tax_rate numeric(5,2) NOT NULL,
    invoice_date date NOT NULL,
    status character varying(32) NOT NULL,
    match_status character varying(32) NOT NULL,
    difference_amount numeric(14,2) DEFAULT 0 NOT NULL,
    remark character varying(500),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL,
    payable_id uuid,
    receipt_id uuid,
    matched_amount numeric(14,2) DEFAULT 0 NOT NULL,
    approval_status character varying(32) DEFAULT 'APPROVED'::character varying NOT NULL,
    verification_status character varying(32) DEFAULT 'VERIFIED'::character varying NOT NULL,
    client_request_id character varying(80),
    approved_by_name character varying(80),
    approved_at timestamp with time zone,
    attachment_document_id uuid
);


--
-- Name: procurement_supplier_quote_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_supplier_quote_lines (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    quote_id uuid NOT NULL,
    request_id uuid NOT NULL,
    quantity numeric(14,2) NOT NULL,
    unit_price numeric(14,2) NOT NULL,
    tax_rate numeric(5,2) NOT NULL,
    delivery_date date,
    remark character varying(500),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: procurement_supplier_quotes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_supplier_quotes (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    inquiry_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    unit_price numeric(14,2) NOT NULL,
    tax_rate numeric(5,2) NOT NULL,
    delivery_date date,
    payment_terms character varying(180),
    remark character varying(500),
    selected boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL,
    currency character varying(8) DEFAULT 'CNY'::character varying NOT NULL,
    freight_amount numeric(14,2) DEFAULT 0 NOT NULL,
    other_cost_amount numeric(14,2) DEFAULT 0 NOT NULL,
    technical_score numeric(5,2) DEFAULT 100 NOT NULL,
    commercial_score numeric(5,2) DEFAULT 100 NOT NULL,
    total_score numeric(5,2) DEFAULT 100 NOT NULL,
    valid_until date
);


--
-- Name: procurement_supplier_reviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_supplier_reviews (
    id uuid NOT NULL,
    tenant_id character varying(64) NOT NULL,
    supplier_id uuid NOT NULL,
    review_period character varying(20) NOT NULL,
    on_time_rate numeric(5,2) DEFAULT 0 NOT NULL,
    quality_rate numeric(5,2) DEFAULT 0 NOT NULL,
    invoice_match_rate numeric(5,2) DEFAULT 0 NOT NULL,
    response_score numeric(5,2) DEFAULT 100 NOT NULL,
    total_score numeric(5,2) DEFAULT 0 NOT NULL,
    grade character varying(20) NOT NULL,
    reviewer_name character varying(80) NOT NULL,
    improvement_action character varying(1000),
    status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: procurement_suppliers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.procurement_suppliers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(160) NOT NULL,
    category character varying(80),
    contact_name character varying(80),
    phone character varying(40),
    settlement_terms character varying(160),
    risk_status character varying(32) DEFAULT 'NORMAL'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    admission_status character varying(40),
    bank_account character varying(512),
    bank_name character varying(120),
    business_scope character varying(800),
    legal_representative character varying(80),
    license_valid_to date,
    qualification_valid_to date,
    registered_address character varying(240),
    registered_capital character varying(80),
    remark character varying(1000),
    taxpayer_type character varying(80),
    unified_social_credit_code character varying(80),
    version bigint DEFAULT 0 NOT NULL,
    admission_submitted_at timestamp with time zone,
    admission_reviewed_at timestamp with time zone,
    admission_reviewer_name character varying(80),
    admission_review_comment character varying(500)
);


--
-- Name: project_budget_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_budget_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    project_id uuid NOT NULL,
    category character varying(40) NOT NULL,
    planned_amount numeric(14,2) NOT NULL,
    remark character varying(300),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: project_cost_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_cost_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    project_id uuid NOT NULL,
    category character varying(40) NOT NULL,
    source_type character varying(40) NOT NULL,
    source_no character varying(80),
    description character varying(300) NOT NULL,
    amount numeric(14,2) NOT NULL,
    incurred_date date NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: project_projects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_projects (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    customer_id uuid,
    code character varying(64) NOT NULL,
    name character varying(180) NOT NULL,
    stage character varying(40) NOT NULL,
    budget_amount numeric(14,2) DEFAULT 0 NOT NULL,
    actual_cost numeric(14,2) DEFAULT 0 NOT NULL,
    progress integer DEFAULT 0 NOT NULL,
    warranty_end_date date,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    project_type character varying(40) DEFAULT 'RENOVATION'::character varying NOT NULL,
    manager_name character varying(80) DEFAULT '历史项目负责人'::character varying NOT NULL,
    site_address character varying(300) DEFAULT '历史项目地址'::character varying NOT NULL,
    contract_amount numeric(14,2) DEFAULT 0 NOT NULL,
    planned_start_date date,
    planned_end_date date,
    approval_status character varying(32) DEFAULT 'APPROVED'::character varying NOT NULL,
    approval_comment character varying(500),
    approver_name character varying(80),
    approved_at timestamp with time zone,
    contract_id uuid,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: project_stage_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_stage_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    project_id uuid NOT NULL,
    from_stage character varying(40) NOT NULL,
    to_stage character varying(40) NOT NULL,
    progress integer NOT NULL,
    comment character varying(500) NOT NULL,
    operator_name character varying(80) NOT NULL,
    changed_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: qual_company_qualifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qual_company_qualifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    external_id character varying(100) NOT NULL,
    subject_company character varying(160) NOT NULL,
    name character varying(200) NOT NULL,
    category character varying(100) NOT NULL,
    level_name character varying(160),
    certificate_no character varying(180),
    issuer character varying(240),
    issue_date date,
    valid_from date,
    valid_to date,
    annual_review_date date,
    renewal_date date,
    scope_text character varying(4000),
    project_types_json character varying(2000),
    holder_branch character varying(240),
    storage_location character varying(500),
    available_for_tender boolean DEFAULT true NOT NULL,
    manual_status character varying(32) DEFAULT 'NORMAL'::character varying NOT NULL,
    locked boolean DEFAULT false NOT NULL,
    attachments_json character varying(4000),
    remark character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: qual_employees; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qual_employees (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    external_id character varying(100) NOT NULL,
    name character varying(80) NOT NULL,
    work_no character varying(64),
    department character varying(120),
    position_name character varying(120),
    id_card character varying(512),
    phone character varying(40),
    entry_date date,
    employment_status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    contract_start date,
    contract_end date,
    social_security_unit character varying(160),
    social_security_start date,
    social_security_end date,
    remark character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    system_user_id uuid,
    organization_id uuid,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: qual_performances; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qual_performances (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    external_id character varying(100) NOT NULL,
    subject_company character varying(160) NOT NULL,
    name character varying(500) NOT NULL,
    client_name character varying(240),
    contract_no character varying(160),
    contract_date date,
    contract_amount character varying(100),
    project_type character varying(160),
    attachments_json character varying(4000),
    remark character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: qual_personnel_certificates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.qual_personnel_certificates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    external_id character varying(100) NOT NULL,
    employee_id uuid NOT NULL,
    name character varying(200) NOT NULL,
    certificate_type character varying(100),
    certificate_no character varying(180),
    specialty character varying(180),
    company_registered boolean DEFAULT false NOT NULL,
    issue_date date,
    valid_to date,
    review_date date,
    available_for_tender boolean DEFAULT true NOT NULL,
    manual_status character varying(32) DEFAULT 'NORMAL'::character varying NOT NULL,
    locked boolean DEFAULT false NOT NULL,
    attachments_json character varying(4000),
    remark character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: risk_rule_configs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.risk_rule_configs (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    default_owner character varying(80),
    enabled boolean NOT NULL,
    escalation_owner character varying(80),
    high_threshold numeric(18,4),
    medium_threshold numeric(18,4),
    module character varying(40) NOT NULL,
    name character varying(120) NOT NULL,
    remark character varying(500),
    rule_code character varying(80) NOT NULL,
    sla_hours integer,
    warning_days integer,
    version bigint NOT NULL
);


--
-- Name: risk_snapshots; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.risk_snapshots (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    amount numeric(18,2),
    module character varying(40) NOT NULL,
    module_name character varying(80) NOT NULL,
    risk_key character varying(180) NOT NULL,
    severity character varying(20) NOT NULL,
    snapshot_date date NOT NULL,
    status character varying(32) NOT NULL,
    subject character varying(300) NOT NULL,
    title character varying(180) NOT NULL,
    workflow_status character varying(32) NOT NULL,
    version bigint NOT NULL
);


--
-- Name: risk_workflow_actions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.risk_workflow_actions (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    from_status character varying(32),
    handling_hours integer,
    note character varying(1000),
    operator_name character varying(80),
    owner character varying(80),
    prevention_action character varying(1000),
    reason character varying(500),
    recurrence boolean,
    responsible_department character varying(120),
    risk_key character varying(180) NOT NULL,
    root_cause character varying(1000),
    to_status character varying(32) NOT NULL,
    version bigint NOT NULL
);


--
-- Name: risk_workflows; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.risk_workflows (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    handling_hours integer,
    note character varying(1000),
    owner character varying(80),
    prevention_action character varying(1000),
    processed_at timestamp(6) with time zone,
    reason character varying(500),
    recurrence boolean,
    responsible_department character varying(120),
    risk_key character varying(180) NOT NULL,
    root_cause character varying(1000),
    status character varying(32) NOT NULL,
    updated_by_name character varying(80),
    version bigint NOT NULL
);


--
-- Name: shedlock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shedlock (
    name character varying(64) NOT NULL,
    lock_until timestamp without time zone NOT NULL,
    locked_at timestamp without time zone NOT NULL,
    locked_by character varying(255) NOT NULL
);


--
-- Name: sys_organizations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_organizations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(120) NOT NULL,
    type character varying(40) DEFAULT 'DEPARTMENT'::character varying,
    sort_order integer DEFAULT 0,
    leader_name character varying(80),
    phone character varying(40),
    enabled boolean DEFAULT true NOT NULL,
    description character varying(500),
    parent_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: sys_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_permissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(120) NOT NULL,
    name character varying(120) NOT NULL,
    module character varying(80) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    built_in boolean DEFAULT false NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: sys_role_data_organizations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_role_data_organizations (
    role_id uuid NOT NULL,
    organization_id uuid NOT NULL
);


--
-- Name: sys_role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_role_permissions (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL
);


--
-- Name: sys_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(120) NOT NULL,
    data_scope character varying(40) DEFAULT 'SELF'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    built_in boolean DEFAULT false NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: sys_soft_delete_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_soft_delete_records (
    id uuid NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    entity_type character varying(80) NOT NULL,
    entity_id uuid NOT NULL,
    title character varying(240) NOT NULL,
    status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    requested_by character varying(120),
    requested_role_codes character varying(500),
    requested_at timestamp with time zone DEFAULT now() NOT NULL,
    approval_id uuid,
    approved_by character varying(120),
    approved_at timestamp with time zone,
    restored_by character varying(120),
    restored_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: sys_user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL
);


--
-- Name: sys_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    org_id uuid,
    username character varying(80) NOT NULL,
    display_name character varying(80) NOT NULL,
    password_hash character varying(255),
    phone character varying(40),
    email character varying(120),
    enabled boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: system_audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.system_audit_logs (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(64),
    tenant_id character varying(64) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(64),
    client_ip character varying(50),
    duration_ms bigint NOT NULL,
    http_method character varying(10) NOT NULL,
    request_path character varying(500) NOT NULL,
    response_status integer NOT NULL,
    username character varying(80),
    biz_module character varying(80),
    biz_object character varying(120),
    operation_type character varying(40),
    query_string character varying(1000),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: system_notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.system_notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    type character varying(40) NOT NULL,
    title character varying(180) NOT NULL,
    content character varying(1000) NOT NULL,
    target_user_id uuid,
    related_type character varying(80),
    related_id uuid,
    dedup_key character varying(180),
    is_read boolean DEFAULT false NOT NULL,
    read_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: work_order_materials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.work_order_materials (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    work_order_id uuid NOT NULL,
    part_id uuid NOT NULL,
    part_name character varying(160) NOT NULL,
    quantity numeric(14,2) NOT NULL,
    unit_cost numeric(14,2) NOT NULL,
    amount numeric(14,2) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: work_order_status_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.work_order_status_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    work_order_id uuid NOT NULL,
    from_status character varying(40),
    to_status character varying(40) NOT NULL,
    operator_name character varying(80) NOT NULL,
    remark character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    version bigint NOT NULL
);


--
-- Name: work_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.work_orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id character varying(64) DEFAULT 'default'::character varying NOT NULL,
    customer_id uuid,
    contract_id uuid,
    project_id uuid,
    code character varying(64) NOT NULL,
    source character varying(80) NOT NULL,
    work_type character varying(80) NOT NULL,
    priority character varying(40) NOT NULL,
    status character varying(40) NOT NULL,
    equipment_name character varying(160),
    engineer_name character varying(80),
    required_cert character varying(120),
    cost_amount numeric(14,2) DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64),
    updated_by character varying(64),
    equipment_id uuid,
    maintenance_plan_id uuid,
    title character varying(180) DEFAULT '服务工单'::character varying NOT NULL,
    planned_date date,
    site_address character varying(300),
    problem_description character varying(1000),
    assignee_id uuid,
    check_in_at timestamp with time zone,
    check_in_location character varying(300),
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    accepted_at timestamp with time zone,
    customer_signer character varying(80),
    service_result character varying(1500),
    acceptance_note character varying(500),
    labor_hours numeric(10,2) DEFAULT 0 NOT NULL,
    labor_cost numeric(14,2) DEFAULT 0 NOT NULL,
    material_cost numeric(14,2) DEFAULT 0 NOT NULL,
    travel_cost numeric(14,2) DEFAULT 0 NOT NULL,
    outsourcing_cost numeric(14,2) DEFAULT 0 NOT NULL,
    billable_amount numeric(14,2) DEFAULT 0 NOT NULL,
    free_warranty boolean DEFAULT false NOT NULL,
    version bigint NOT NULL
);


--
-- Data for Name: approval_assignee_configs; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('9ecd6ddd-3d17-442b-8294-1a342336bcc3', '2026-07-14 13:51:40.839762+08', NULL, 'default', '2026-07-14 13:51:40.839762+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, NULL, 200, NULL, '合同签署前确认客户、金额、收款和交付条款', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('af2218ee-6892-4328-8701-688bc17d95aa', '2026-07-14 13:51:40.839785+08', NULL, 'default', '2026-07-14 13:51:40.839785+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, 100000.00, 100, NULL, '合同金额达到 10 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('3e5a1528-ebb4-4f73-9bf4-7db029690dc2', '2026-07-14 13:51:40.839807+08', NULL, 'default', '2026-07-14 13:51:40.839807+08', NULL, 'SEQUENTIAL', '变更', 'BUSINESS_TYPE', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, NULL, 100, NULL, '合同变更需复核范围、金额和履约影响', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('f8df0ad5-b4c6-454c-84bb-0b69f5310237', '2026-07-14 13:51:40.839835+08', NULL, 'default', '2026-07-14 13:51:40.839835+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, NULL, 200, NULL, '采购申请由部门/项目负责人审核必要性和预算归属', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('2831c998-040d-484e-ba2c-fcecc517a942', '2026-07-14 13:51:40.839857+08', NULL, 'default', '2026-07-14 13:51:40.839857+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, 50000.00, 100, NULL, '采购金额达到 5 万元以上，升级采购负责人和财务复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('cb67415a-a00c-4b40-9a36-6cc5f616afd2', '2026-07-14 13:51:40.839879+08', NULL, 'default', '2026-07-14 13:51:40.839879+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, NULL, 90, NULL, '高风险供应商采购需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('2c23330e-9b6c-463a-bdbf-5f4038e6665d', '2026-07-14 13:51:40.839898+08', NULL, 'default', '2026-07-14 13:51:40.839898+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, NULL, 200, NULL, '项目立项先审核范围、预算、计划和责任部门', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('f4107cd2-0cb7-444b-964a-810f9ee08bdb', '2026-07-14 13:51:40.839602+08', NULL, 'default', '2026-07-14 15:47:08.693299+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'QUOTE', '报价审批', NULL, NULL, 200, NULL, '报价提交后由销售负责人先审报价完整性、毛利和交付风险', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, 'PUBLISHED', NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('267facfd-b78f-48d3-98bb-965dbf05d78b', '2026-07-14 13:51:40.839688+08', NULL, 'default', '2026-07-14 15:47:08.693866+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'QUOTE', '报价审批', NULL, 50000.00, 100, NULL, '报价金额达到 5 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, 'PUBLISHED', NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('c530a102-830a-4422-b3e0-3d3508acba97', '2026-07-14 13:51:40.839732+08', NULL, 'default', '2026-07-14 15:47:08.693888+08', NULL, 'SEQUENTIAL', NULL, 'CUSTOMER_LEVEL', 'VIP', NULL, true, 'QUOTE', '报价审批', NULL, NULL, 90, NULL, '重点客户报价需要负责人复核商务条款', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, 'PUBLISHED', NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('99fe7345-2a28-4de3-af30-0f6ea7fb5795', '2026-07-14 13:51:40.839917+08', NULL, 'default', '2026-07-14 13:51:40.839917+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, 100000.00, 100, NULL, '项目预算达到 10 万元以上，升级经营/财务复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('e6fd41c2-098f-465c-99fe-ed130fbdfa7a', '2026-07-14 13:51:40.839936+08', NULL, 'default', '2026-07-14 13:51:40.839936+08', NULL, 'SEQUENTIAL', '新增项目', 'BUSINESS_TYPE', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, NULL, 110, NULL, '新增项目需确认资源占用和收益测算', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('1e41daff-7484-482e-89b4-2f169a5d0f9f', '2026-07-14 13:51:40.839955+08', NULL, 'default', '2026-07-14 13:51:40.839955+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, NULL, 200, NULL, '付款申请先核对应付、票据、合同和到货信息', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('8dddf8b9-8e9c-4f50-a6ac-17138b5a6b0f', '2026-07-14 13:51:40.83998+08', NULL, 'default', '2026-07-14 13:51:40.83998+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, 50000.00, 100, NULL, '付款金额达到 5 万元以上，升级财务负责人复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('2ce99263-7804-4361-bce2-206e52ccb5d0', '2026-07-14 13:51:40.840066+08', NULL, 'default', '2026-07-14 13:51:40.840066+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, NULL, 90, NULL, '高风险供应商付款需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('6c00bbe7-78ed-414a-a26a-1091cd91fc1c', '2026-07-14 13:51:40.840188+08', NULL, 'default', '2026-07-14 13:51:40.840188+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'EXPENSE', '费用报销审批', NULL, NULL, 200, NULL, '费用报销由直属负责人审核真实性和预算归属', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('546f5fee-27fb-4b50-8058-3068fba34dc1', '2026-07-14 13:51:40.840221+08', NULL, 'default', '2026-07-14 13:51:40.840221+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'EXPENSE', '费用报销审批', NULL, 10000.00, 100, NULL, '单笔报销达到 1 万元以上，升级财务复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('dede9e2a-82eb-42f0-8eb0-bebadbf08c69', '2026-07-14 13:51:40.840243+08', NULL, 'default', '2026-07-14 13:51:40.840243+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'OUTSOURCE', '外包服务审批', NULL, NULL, 200, NULL, '外包服务先审核供应商、服务范围和验收口径', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('1f8690a1-798b-4d28-9245-abbfa3407ba7', '2026-07-14 13:51:40.84027+08', NULL, 'default', '2026-07-14 13:51:40.84027+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'OUTSOURCE', '外包服务审批', NULL, 30000.00, 100, NULL, '外包金额达到 3 万元以上，升级负责人复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('34b92cc0-e32a-427e-a2d3-7cfe4ab3b283', '2026-07-14 13:51:40.840317+08', NULL, 'default', '2026-07-14 13:51:40.840317+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'OUTSOURCE', '外包服务审批', NULL, NULL, 90, NULL, '高风险供应商外包需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('63414d16-59d6-49e4-a188-eebd4e3e4357', '2026-07-14 13:51:40.840338+08', NULL, 'default', '2026-07-14 13:51:40.840338+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'LEAVE', '请假审批', NULL, NULL, 200, NULL, '员工请假由直属负责人审核工作安排', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('55626482-c699-4a45-8ce0-92dcf590f4a5', '2026-07-14 13:51:40.840358+08', NULL, 'default', '2026-07-14 13:51:40.840358+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'LEAVE', '请假审批', NULL, 3.00, 100, NULL, '请假 3 天及以上，升级人事复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('83341849-821a-4ebe-88ae-11f24cec0da5', '2026-07-14 13:51:40.84038+08', NULL, 'default', '2026-07-14 13:51:40.84038+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'TRAVEL', '出差审批', NULL, NULL, 200, NULL, '出差申请先审核出差必要性、周期和预算', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('402287f6-d80e-4948-b31b-570f606064af', '2026-07-14 13:51:40.840402+08', NULL, 'default', '2026-07-14 13:51:40.840402+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'TRAVEL', '出差审批', NULL, 5000.00, 100, NULL, '出差预算达到 5000 元以上，升级财务复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('31a06ca9-f03f-4e1d-8aba-d861e3e4392a', '2026-07-14 13:51:40.840419+08', NULL, 'default', '2026-07-14 13:51:40.840419+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'SEAL', '用印审批', NULL, NULL, 200, NULL, '用印申请审核文件类型、主体和授权依据', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('7f50ab8f-fe90-4c17-afb0-bf63df5302a7', '2026-07-14 13:51:40.840436+08', NULL, 'default', '2026-07-14 13:51:40.840436+08', NULL, 'SEQUENTIAL', '合同', 'BUSINESS_TYPE', NULL, NULL, true, 'SEAL', '用印审批', NULL, NULL, 100, NULL, '合同类用印需负责人复核合同审批状态', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('82b57e81-9142-41bc-bd1a-67416735aa96', '2026-07-14 13:51:40.840456+08', NULL, 'default', '2026-07-14 13:51:40.840456+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'OTHER', '通用审批', NULL, NULL, 200, NULL, '通用事项默认负责人审批', 1, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('2f268de7-7788-46b4-9b81-71fdb5dcd455', '2026-07-14 13:51:40.840479+08', NULL, 'default', '2026-07-14 13:51:40.840479+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'OTHER', '通用审批', NULL, 20000.00, 100, NULL, '涉及金额达到 2 万元以上，升级管理层复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 1, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('42fec2e3-d6f2-4241-a80c-f23bb5d86f7c', '2026-07-17 12:58:23.846204+08', NULL, 'default', '2026-07-17 12:58:23.846204+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, 100000.00, 100, NULL, '合同金额达到 10 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('49277bda-4d80-44b1-b455-61179101eaab', '2026-07-17 12:58:23.846283+08', NULL, 'default', '2026-07-17 12:58:23.846283+08', NULL, 'SEQUENTIAL', '变更', 'BUSINESS_TYPE', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, NULL, 100, NULL, '合同变更需复核范围、金额和履约影响', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('dde2b53e-ec26-4459-838f-69b1e4315d0c', '2026-07-17 12:58:23.846304+08', NULL, 'default', '2026-07-17 12:58:23.846304+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, NULL, 200, NULL, '合同签署前确认客户、金额、收款和交付条款', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 2, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('0117376f-9ecf-4501-b1f3-b0ecb994ad68', '2026-07-17 12:58:23.871781+08', NULL, 'default', '2026-07-17 12:58:23.871781+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, 100000.00, 100, NULL, '合同金额达到 10 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('54423847-1a1a-49d8-b0d4-ea7d27f3b0d3', '2026-07-17 12:58:23.871849+08', NULL, 'default', '2026-07-17 12:58:23.871849+08', NULL, 'SEQUENTIAL', '变更', 'BUSINESS_TYPE', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, NULL, 100, NULL, '合同变更需复核范围、金额和履约影响', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('3e46cce3-ac3d-41de-a808-00a5ed3d4677', '2026-07-17 12:58:23.871882+08', NULL, 'default', '2026-07-17 12:58:23.871882+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, NULL, 200, NULL, '合同签署前确认客户、金额、收款和交付条款', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('9d522541-9939-4dc4-bc1f-5de68089525b', '2026-07-17 12:58:23.871898+08', NULL, 'default', '2026-07-17 12:58:23.871898+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, 100000.00, 100, NULL, '合同金额达到 10 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('7f7b01a9-c3f3-4c33-8877-29782f117971', '2026-07-17 12:58:23.897409+08', NULL, 'default', '2026-07-17 12:58:23.897409+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, 100000.00, 100, NULL, '合同金额达到 10 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 4, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('50145ec6-2b04-414b-85be-6862ea54c605', '2026-07-17 12:58:23.89746+08', NULL, 'default', '2026-07-17 12:58:23.89746+08', NULL, 'SEQUENTIAL', '变更', 'BUSINESS_TYPE', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, NULL, 100, NULL, '合同变更需复核范围、金额和履约影响', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 4, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('9c88829b-4c93-45be-87e8-8ad39ddcd0b3', '2026-07-17 12:58:23.897484+08', NULL, 'default', '2026-07-17 12:58:23.897484+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, NULL, 200, NULL, '合同签署前确认客户、金额、收款和交付条款', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('0e01286d-3d13-46c9-aa57-1177662aff7d', '2026-07-17 12:58:23.897507+08', NULL, 'default', '2026-07-17 12:58:23.897507+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, 100000.00, 100, NULL, '合同金额达到 10 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('4b06db12-e799-4c58-a99f-78f4aaadec39', '2026-07-17 12:58:23.89752+08', NULL, 'default', '2026-07-17 12:58:23.89752+08', NULL, 'SEQUENTIAL', '变更', 'BUSINESS_TYPE', NULL, NULL, true, 'CONTRACT', '合同审批', NULL, NULL, 100, NULL, '合同变更需复核范围、金额和履约影响', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('a917d391-44bc-4bf1-9067-bb50cea5d143', '2026-07-17 12:58:23.924315+08', NULL, 'default', '2026-07-17 12:58:23.924315+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'LEAVE', '请假审批', NULL, 3.00, 100, NULL, '请假 3 天及以上，升级人事复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('682cd9c6-aafa-46ae-a30a-6f0f50418cbc', '2026-07-17 12:58:23.92436+08', NULL, 'default', '2026-07-17 12:58:23.92436+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'LEAVE', '请假审批', NULL, NULL, 200, NULL, '员工请假由直属负责人审核工作安排', 1, NULL, NULL, 'ROLE', '1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', NULL, NULL, NULL, NULL, 2, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('916ebbc4-08bd-4d72-814b-370308d737be', '2026-07-17 12:58:23.945599+08', NULL, 'default', '2026-07-17 12:58:23.945599+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'LEAVE', '请假审批', NULL, 3.00, 100, NULL, '请假 3 天及以上，升级人事复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('33c8f295-afb3-458d-892e-b1ce90406f12', '2026-07-17 12:58:23.945652+08', NULL, 'default', '2026-07-17 12:58:23.945652+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'LEAVE', '请假审批', NULL, NULL, 200, NULL, '员工请假由直属负责人审核工作安排', 1, NULL, NULL, 'ROLE', '1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('ee1a42bf-c2a0-449f-8bbf-d509d8ce8358', '2026-07-17 12:58:23.945667+08', NULL, 'default', '2026-07-17 12:58:23.945667+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'LEAVE', '请假审批', NULL, 3.00, 100, NULL, '请假 3 天及以上，升级人事复核', 2, NULL, NULL, 'ROLE', '1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('dc5b8f9f-98e6-4670-a463-f6540b45690e', '2026-07-17 12:58:23.967288+08', NULL, 'default', '2026-07-17 12:58:23.967288+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'OTHER', '通用审批', NULL, 20000.00, 100, NULL, '涉及金额达到 2 万元以上，升级管理层复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('e9193946-e5d8-48cc-9061-2e9c7922c94f', '2026-07-17 12:58:23.967343+08', NULL, 'default', '2026-07-17 12:58:23.967343+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'OTHER', '通用审批', NULL, NULL, 200, NULL, '通用事项默认负责人审批', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 2, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('f498d4df-f8f0-480d-a618-e49e14ec97f2', '2026-07-17 12:58:23.987775+08', NULL, 'default', '2026-07-17 12:58:23.987775+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'OTHER', '通用审批', NULL, 20000.00, 100, NULL, '涉及金额达到 2 万元以上，升级管理层复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('8b38ceb5-ed47-4710-89d4-35f2cd4a1c46', '2026-07-17 12:58:23.98783+08', NULL, 'default', '2026-07-17 12:58:23.98783+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'OTHER', '通用审批', NULL, NULL, 200, NULL, '通用事项默认负责人审批', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('1a795d1b-3de7-4d26-b417-449eb59e41ee', '2026-07-17 12:58:23.987847+08', NULL, 'default', '2026-07-17 12:58:23.987847+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'OTHER', '通用审批', NULL, 20000.00, 100, NULL, '涉及金额达到 2 万元以上，升级管理层复核', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('c5d2e59e-5d37-4646-8daf-ed0714b34130', '2026-07-17 12:58:24.013588+08', NULL, 'default', '2026-07-17 12:58:24.013588+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, 50000.00, 100, NULL, '付款金额达到 5 万元以上，升级财务负责人复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('af0bb428-58df-4f78-8161-6a613256dc2d', '2026-07-17 12:58:24.013633+08', NULL, 'default', '2026-07-17 12:58:24.013633+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, NULL, 90, NULL, '高风险供应商付款需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('f1973b56-e147-4ed4-a019-6992fcbe9ba6', '2026-07-17 12:58:24.013643+08', NULL, 'default', '2026-07-17 12:58:24.013643+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, NULL, 200, NULL, '付款申请先核对应付、票据、合同和到货信息', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 2, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('8edfa4a9-3046-4ad4-afe6-7c28d6443947', '2026-07-17 12:58:24.031757+08', NULL, 'default', '2026-07-17 12:58:24.031757+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, 50000.00, 100, NULL, '付款金额达到 5 万元以上，升级财务负责人复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('0ee54bc9-6fe6-4e60-ac94-d3081c3d46d5', '2026-07-17 12:58:24.031791+08', NULL, 'default', '2026-07-17 12:58:24.031791+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, NULL, 90, NULL, '高风险供应商付款需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('4510248f-35b8-4035-a5a2-ba07d8a8c8b5', '2026-07-17 12:58:24.031799+08', NULL, 'default', '2026-07-17 12:58:24.031799+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, NULL, 200, NULL, '付款申请先核对应付、票据、合同和到货信息', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('30ce7aea-c941-44b0-bbed-359ca86333a3', '2026-07-17 12:58:24.031805+08', NULL, 'default', '2026-07-17 12:58:24.031805+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, NULL, 90, NULL, '高风险供应商付款需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('f3f225f3-1163-4894-bc51-c77f11a4d981', '2026-07-17 12:58:24.05213+08', NULL, 'default', '2026-07-17 12:58:24.05213+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, 50000.00, 100, NULL, '付款金额达到 5 万元以上，升级财务负责人复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 4, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('de806746-6572-4f1e-b8e1-d0f53c5569ff', '2026-07-17 12:58:24.052187+08', NULL, 'default', '2026-07-17 12:58:24.052187+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, NULL, 90, NULL, '高风险供应商付款需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 4, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('57f8ab55-9855-4058-8a0a-5deaa6835f98', '2026-07-17 12:58:24.052236+08', NULL, 'default', '2026-07-17 12:58:24.052236+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, NULL, 200, NULL, '付款申请先核对应付、票据、合同和到货信息', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('5fc1a299-6c8e-4f38-b163-3c4e03ade89c', '2026-07-17 12:58:24.052254+08', NULL, 'default', '2026-07-17 12:58:24.052254+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, NULL, 90, NULL, '高风险供应商付款需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('706caa23-7768-4170-a282-0586df397fb2', '2026-07-17 12:58:24.052268+08', NULL, 'default', '2026-07-17 12:58:24.052268+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PAYMENT', '付款申请审批', NULL, 50000.00, 100, NULL, '付款金额达到 5 万元以上，升级财务负责人复核', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('9b1762bc-0cc0-4e74-a35a-5eac76b59591', '2026-07-17 12:58:24.077781+08', NULL, 'default', '2026-07-17 12:58:24.077781+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, 100000.00, 100, NULL, '项目预算达到 10 万元以上，升级经营/财务复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('a0d478e1-8a75-4f1b-8053-bc72d78be894', '2026-07-17 12:58:24.077832+08', NULL, 'default', '2026-07-17 12:58:24.077832+08', NULL, 'SEQUENTIAL', '新增项目', 'BUSINESS_TYPE', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, NULL, 110, NULL, '新增项目需确认资源占用和收益测算', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('6fb100e1-f18f-4560-b549-c50244854239', '2026-07-17 12:58:24.077864+08', NULL, 'default', '2026-07-17 12:58:24.077864+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, NULL, 200, NULL, '项目立项先审核范围、预算、计划和责任部门', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 2, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('2e5dabd6-a4fa-40eb-8351-7821af5f26c3', '2026-07-17 12:58:24.097319+08', NULL, 'default', '2026-07-17 12:58:24.097319+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, 100000.00, 100, NULL, '项目预算达到 10 万元以上，升级经营/财务复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('6a84c125-d77a-4b63-b9c0-9ffdc5b7fc6c', '2026-07-17 12:58:24.09737+08', NULL, 'default', '2026-07-17 12:58:24.09737+08', NULL, 'SEQUENTIAL', '新增项目', 'BUSINESS_TYPE', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, NULL, 110, NULL, '新增项目需确认资源占用和收益测算', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('a3efb6b5-1db7-457c-a952-ece809846ec0', '2026-07-17 12:58:24.097385+08', NULL, 'default', '2026-07-17 12:58:24.097385+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, NULL, 200, NULL, '项目立项先审核范围、预算、计划和责任部门', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('7528dbf1-4ec6-4285-acde-25e2ad4b4e86', '2026-07-17 12:58:24.097399+08', NULL, 'default', '2026-07-17 12:58:24.097399+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, 100000.00, 100, NULL, '项目预算达到 10 万元以上，升级经营/财务复核', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('b6657b60-2cac-4ea2-983b-c0879a2d6a31', '2026-07-17 12:58:24.119881+08', NULL, 'default', '2026-07-17 12:58:24.119881+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, 100000.00, 100, NULL, '项目预算达到 10 万元以上，升级经营/财务复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 4, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('525fdbb3-941b-4989-8b14-75f886ef8189', '2026-07-17 12:58:24.119923+08', NULL, 'default', '2026-07-17 12:58:24.119923+08', NULL, 'SEQUENTIAL', '新增项目', 'BUSINESS_TYPE', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, NULL, 110, NULL, '新增项目需确认资源占用和收益测算', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 4, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('b5bd9de3-143e-4b55-95b1-c78bd4496e8e', '2026-07-17 12:58:24.119931+08', NULL, 'default', '2026-07-17 12:58:24.119931+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, NULL, 200, NULL, '项目立项先审核范围、预算、计划和责任部门', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('aa6d28e6-22cb-4882-aaf3-8cbffd7abcac', '2026-07-17 12:58:24.119938+08', NULL, 'default', '2026-07-17 12:58:24.119938+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, 100000.00, 100, NULL, '项目预算达到 10 万元以上，升级经营/财务复核', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('e6e7c7d8-3360-4802-bc39-1a8467853162', '2026-07-17 12:58:24.119944+08', NULL, 'default', '2026-07-17 12:58:24.119944+08', NULL, 'SEQUENTIAL', '新增项目', 'BUSINESS_TYPE', NULL, NULL, true, 'PROJECT', '项目立项审批', NULL, NULL, 110, NULL, '新增项目需确认资源占用和收益测算', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('594dd0b2-d3c1-46aa-9c78-1b0426ff45eb', '2026-07-17 12:58:24.144546+08', NULL, 'default', '2026-07-17 12:58:24.144546+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, 50000.00, 100, NULL, '采购金额达到 5 万元以上，升级采购负责人和财务复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('0c7d96e6-a453-4066-948b-7db4dc71aa4d', '2026-07-17 12:58:24.144642+08', NULL, 'default', '2026-07-17 12:58:24.144642+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, NULL, 90, NULL, '高风险供应商采购需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('7a3ada77-4ee7-495f-b82a-7a7b4a4056a3', '2026-07-17 12:58:24.144659+08', NULL, 'default', '2026-07-17 12:58:24.144659+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, NULL, 200, NULL, '采购申请由部门/项目负责人审核必要性和预算归属', 1, NULL, NULL, 'ROLE', '5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', NULL, NULL, NULL, NULL, 2, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('60319865-7981-46f1-9912-0a3a9734dec4', '2026-07-17 12:58:24.165789+08', NULL, 'default', '2026-07-17 12:58:24.165789+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, 50000.00, 100, NULL, '采购金额达到 5 万元以上，升级采购负责人和财务复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('02626d16-c43a-4d1c-9753-82ecf2c9eef8', '2026-07-17 12:58:24.16626+08', NULL, 'default', '2026-07-17 12:58:24.16626+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, NULL, 90, NULL, '高风险供应商采购需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('4c251ad1-4922-4ace-ab87-3edf452dc8bd', '2026-07-17 12:58:24.1663+08', NULL, 'default', '2026-07-17 12:58:24.1663+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, NULL, 200, NULL, '采购申请由部门/项目负责人审核必要性和预算归属', 1, NULL, NULL, 'ROLE', '5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('dc34048a-b0c9-446f-85bf-1a7e3eef2c43', '2026-07-17 12:58:24.166318+08', NULL, 'default', '2026-07-17 12:58:24.166318+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, NULL, 90, NULL, '高风险供应商采购需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('7f3338ce-33a9-485f-a367-4afedac18d68', '2026-07-17 12:58:24.190158+08', NULL, 'default', '2026-07-17 12:58:24.190158+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, 50000.00, 100, NULL, '采购金额达到 5 万元以上，升级采购负责人和财务复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 4, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('06522e0b-e5a6-423f-bd10-a96482d99bc8', '2026-07-17 12:58:24.190222+08', NULL, 'default', '2026-07-17 12:58:24.190222+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, NULL, 90, NULL, '高风险供应商采购需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 4, NULL, NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('59781011-4e73-4401-9f72-bc1ee3c33ab5', '2026-07-17 12:58:24.19026+08', NULL, 'default', '2026-07-17 12:58:24.19026+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, NULL, 200, NULL, '采购申请由部门/项目负责人审核必要性和预算归属', 1, NULL, NULL, 'ROLE', '5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('7a0c13ef-a844-4495-b101-8b3ff666c56c', '2026-07-17 12:58:24.190278+08', NULL, 'default', '2026-07-17 12:58:24.190278+08', NULL, 'SEQUENTIAL', NULL, 'SUPPLIER_RISK', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, NULL, 90, NULL, '高风险供应商采购需要负责人复核', 2, 'HIGH', NULL, 'ROLE', '5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('8ee7498f-8f37-4c17-82d9-582289aab424', '2026-07-17 12:58:24.190299+08', NULL, 'default', '2026-07-17 12:58:24.190299+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'PURCHASE', '采购申请审批', NULL, 50000.00, 100, NULL, '采购金额达到 5 万元以上，升级采购负责人和财务复核', 2, NULL, NULL, 'ROLE', '5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('281b96f8-79d0-45e3-9a97-cbf84efdf862', '2026-07-17 12:58:24.213526+08', NULL, 'default', '2026-07-17 12:58:24.213526+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'QUOTE', '报价审批', NULL, 50000.00, 100, NULL, '报价金额达到 5 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, 'PUBLISHED', NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('968e92b0-ef51-4800-adf0-b0733398ab0a', '2026-07-17 12:58:24.213579+08', NULL, 'default', '2026-07-17 12:58:24.213579+08', NULL, 'SEQUENTIAL', NULL, 'CUSTOMER_LEVEL', 'VIP', NULL, true, 'QUOTE', '报价审批', NULL, NULL, 90, NULL, '重点客户报价需要负责人复核商务条款', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 2, 'PUBLISHED', NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('f5b3d9b0-3efd-4eec-9eca-2e170d305385', '2026-07-17 12:58:24.213594+08', NULL, 'default', '2026-07-17 12:58:24.213594+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'QUOTE', '报价审批', NULL, NULL, 200, NULL, '报价提交后由销售负责人先审报价完整性、毛利和交付风险', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 2, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('b13871d5-93b0-49d4-94f6-a083b88eee43', '2026-07-17 12:58:24.289492+08', NULL, 'default', '2026-07-17 12:58:24.289492+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'QUOTE', '报价审批', NULL, 50000.00, 100, NULL, '报价金额达到 5 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('3a22f560-236e-43cd-ae67-ee69cf739d93', '2026-07-17 12:58:24.289545+08', NULL, 'default', '2026-07-17 12:58:24.289545+08', NULL, 'SEQUENTIAL', NULL, 'CUSTOMER_LEVEL', 'VIP', NULL, true, 'QUOTE', '报价审批', NULL, NULL, 90, NULL, '重点客户报价需要负责人复核商务条款', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('6c0b64df-b3ae-4bd4-ae96-52ee06277390', '2026-07-17 12:58:24.289558+08', NULL, 'default', '2026-07-17 12:58:24.289558+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'QUOTE', '报价审批', NULL, NULL, 200, NULL, '报价提交后由销售负责人先审报价完整性、毛利和交付风险', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('dcf175e9-4bba-4804-b74f-cf3ee0bcd804', '2026-07-17 12:58:24.289571+08', NULL, 'default', '2026-07-17 12:58:24.289571+08', NULL, 'SEQUENTIAL', NULL, 'CUSTOMER_LEVEL', 'VIP', NULL, true, 'QUOTE', '报价审批', NULL, NULL, 90, NULL, '重点客户报价需要负责人复核商务条款', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 3, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('4f8f767f-97ef-4683-913a-07ba19208de0', '2026-07-17 12:58:24.31179+08', NULL, 'default', '2026-07-17 12:58:24.31179+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'QUOTE', '报价审批', NULL, 50000.00, 100, NULL, '报价金额达到 5 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('05df4150-1078-48dd-a15b-5ae690c0a74b', '2026-07-17 12:58:24.311923+08', NULL, 'default', '2026-07-17 12:58:24.311923+08', NULL, 'SEQUENTIAL', NULL, 'CUSTOMER_LEVEL', 'VIP', NULL, true, 'QUOTE', '报价审批', NULL, NULL, 90, NULL, '重点客户报价需要负责人复核商务条款', 2, NULL, NULL, 'ROLE', '00000000-0000-4000-8000-000000000101', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', NULL, 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('4c01ad08-0042-4b8d-b3de-24b915228ca1', '2026-07-17 12:58:24.311945+08', NULL, 'default', '2026-07-17 12:58:24.311945+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'QUOTE', '报价审批', NULL, NULL, 200, NULL, '报价提交后由销售负责人先审报价完整性、毛利和交付风险', 1, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('cd115672-219e-401e-a44e-604878e7b665', '2026-07-17 12:58:24.31198+08', NULL, 'default', '2026-07-17 12:58:24.31198+08', NULL, 'SEQUENTIAL', NULL, 'CUSTOMER_LEVEL', 'VIP', NULL, true, 'QUOTE', '报价审批', NULL, NULL, 90, NULL, '重点客户报价需要负责人复核商务条款', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('9dd32c64-d5df-487c-a5ae-1617667fa27f', '2026-07-17 12:58:24.311995+08', NULL, 'default', '2026-07-17 12:58:24.311995+08', NULL, 'SEQUENTIAL', NULL, 'AMOUNT', NULL, NULL, true, 'QUOTE', '报价审批', NULL, 50000.00, 100, NULL, '报价金额达到 5 万元以上，升级财务/总经办复核', 2, NULL, NULL, 'ROLE', '6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', NULL, NULL, NULL, NULL, 4, 'PUBLISHED', 'ANY_APPROVE', 0);
INSERT INTO public.approval_assignee_configs (id, created_at, created_by, tenant_id, updated_at, updated_by, approval_mode, business_type, condition_type, customer_level, department_name, enabled, flow_code, flow_name, max_amount, min_amount, priority, project_code, remark, sequence_no, supplier_risk, user_id, assignee_type, role_id, auto_action, dynamic_assignee, escalation_role_id, sla_hours, version_no, publish_status, step_policy, version) VALUES ('00000000-0000-4000-8000-000000006201', '2026-07-17 15:23:57.760998+08', NULL, 'default', '2026-07-17 15:23:57.760998+08', NULL, 'SEQUENTIAL', NULL, 'ANY', NULL, NULL, true, 'CONTRACT_CHANGE', '合同变更/盖章件审批', NULL, NULL, 100, NULL, NULL, 1, NULL, NULL, 'DYNAMIC', NULL, NULL, 'CUSTOMER_OWNER', NULL, NULL, 1, 'PUBLISHED', 'ANY_APPROVE', 0);


--
-- Data for Name: sys_organizations; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000011', 'default', 'EXECUTIVE_OFFICE', '总经办', 'DEPARTMENT', 10, NULL, NULL, true, '战略决策支持、经营协调、重大事项督办与管理层服务。', '00000000-0000-4000-8000-000000000001', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000012', 'default', 'MARKET_SALES_CENTER', '市场销售中心', 'DEPARTMENT', 20, NULL, NULL, true, '市场拓展、客户开发、商务管理与收入目标达成。', '00000000-0000-4000-8000-000000000001', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000013', 'default', 'PROJECT_DELIVERY_CENTER', '项目交付中心', 'DEPARTMENT', 30, NULL, NULL, true, '项目计划、技术实施、交付验收和质量管理。', '00000000-0000-4000-8000-000000000001', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000014', 'default', 'SUPPLY_CHAIN_CENTER', '供应链中心', 'DEPARTMENT', 40, NULL, NULL, true, '供应商、采购、仓储物流与物料成本协同。', '00000000-0000-4000-8000-000000000001', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000015', 'default', 'FINANCE_DEPARTMENT', '财务部', 'DEPARTMENT', 50, NULL, NULL, true, '预算、应收应付、资金、税务和经营核算。', '00000000-0000-4000-8000-000000000001', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000016', 'default', 'GENERAL_MANAGEMENT', '综合管理部', 'DEPARTMENT', 60, NULL, NULL, true, '人力、行政、制度、档案和后勤保障。', '00000000-0000-4000-8000-000000000001', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000021', 'default', 'MARKETING_DEPARTMENT', '市场部', 'DEPARTMENT', 10, NULL, NULL, true, '品牌建设、渠道拓展、市场调研和推广活动。', '00000000-0000-4000-8000-000000000012', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000023', 'default', 'CUSTOMER_SERVICE_DEPARTMENT', '客户服务部', 'DEPARTMENT', 30, NULL, NULL, true, '客户接洽、服务协调、回访投诉和满意度管理。', '00000000-0000-4000-8000-000000000012', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000031', 'default', 'PROJECT_MANAGEMENT_DEPARTMENT', '项目管理部', 'DEPARTMENT', 10, NULL, NULL, true, '项目立项、计划进度、成本风险和验收归档。', '00000000-0000-4000-8000-000000000013', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000032', 'default', 'TECHNICAL_SERVICE_DEPARTMENT', '技术服务部', 'DEPARTMENT', 20, NULL, NULL, true, '技术方案、实施支持、现场服务和问题处理。', '00000000-0000-4000-8000-000000000013', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000033', 'default', 'QUALITY_DEPARTMENT', '质量管理部', 'DEPARTMENT', 30, NULL, NULL, true, '质量标准、过程检查、交付评审和持续改进。', '00000000-0000-4000-8000-000000000013', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000041', 'default', 'PROCUREMENT_DEPARTMENT', '采购部', 'DEPARTMENT', 10, NULL, NULL, true, '采购申请、询比价、订单执行和供应商管理。', '00000000-0000-4000-8000-000000000014', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000042', 'default', 'WAREHOUSE_LOGISTICS_DEPARTMENT', '仓储物流部', 'DEPARTMENT', 20, NULL, NULL, true, '入出库、盘点、库位、安全库存和物流协调。', '00000000-0000-4000-8000-000000000014', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000061', 'default', 'HUMAN_RESOURCES_DEPARTMENT', '人力资源部', 'DEPARTMENT', 10, NULL, NULL, true, '招聘、员工档案、合同、考勤、薪酬绩效和培训。', '00000000-0000-4000-8000-000000000016', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000062', 'default', 'ADMINISTRATION_DEPARTMENT', '行政部', 'DEPARTMENT', 20, NULL, NULL, true, '行政事务、资产车辆、用章、会议和办公保障。', '00000000-0000-4000-8000-000000000016', '2026-07-02 11:05:55.233404+08', '2026-07-02 11:05:55.233404+08', NULL, NULL, 0);
INSERT INTO public.sys_organizations (id, tenant_id, code, name, type, sort_order, leader_name, phone, enabled, description, parent_id, created_at, updated_at, created_by, updated_by, version) VALUES ('00000000-0000-4000-8000-000000000001', 'default', 'ROOT', '江苏微炬能源科技有限公司', 'COMPANY', 0, '席向阳', '', true, '公司经营管理总部，统筹战略、经营目标和资源配置。', NULL, '2026-07-02 11:05:54.938609+08', '2026-07-14 13:46:18.194106+08', NULL, NULL, 0);


--
-- Data for Name: sys_permissions; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('918f5f25-cf44-4c07-b151-a55ec1afa36e', 'default', 'crm:dashboard:view', 'CRM仪表盘查看', 'crm', '2026-07-13 17:44:14.815988+08', '2026-07-13 17:44:14.815988+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('6b43ebdb-2c94-464b-b9b2-db8f396a6286', 'default', 'crm:customer:delete', '客户删除', 'crm', '2026-07-13 17:44:14.837669+08', '2026-07-13 17:44:14.837669+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('acfe4c56-d46a-4e35-a79c-6f10e0511a3f', 'default', 'crm:opportunity:delete', '商机删除', 'crm', '2026-07-13 17:44:14.844919+08', '2026-07-13 17:44:14.844919+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('f571c12c-19dd-48f3-b6bc-4537957ff9a1', 'default', 'crm:quote:delete', '报价删除', 'crm', '2026-07-13 17:44:14.852286+08', '2026-07-13 17:44:14.852286+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('717ae4d6-3473-4b54-9f0a-84d4c25bb187', 'default', 'crm:contract:create', '合同创建', 'crm', '2026-07-13 17:44:14.86131+08', '2026-07-13 17:44:14.86131+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('fe59ff2d-b3f7-4194-841b-5a845a828c39', 'default', 'crm:contract:update', '合同修改', 'crm', '2026-07-13 17:44:14.864026+08', '2026-07-13 17:44:14.864026+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('c9e3bb51-ee48-4749-86d7-5ef30c91eba7', 'default', 'crm:contract:delete', '合同删除', 'crm', '2026-07-13 17:44:14.866553+08', '2026-07-13 17:44:14.866553+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('bf49da93-92c9-4145-ac19-a30e696db85e', 'default', 'crm:followup:delete', '跟进删除', 'crm', '2026-07-13 17:44:14.872182+08', '2026-07-13 17:44:14.872182+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('79f5ee96-a718-4957-8ee2-539b4ab2ca2d', 'default', 'crm:receivable:update', '应收修改', 'crm', '2026-07-13 17:44:14.882125+08', '2026-07-13 17:44:14.882125+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('7f2113e8-ca5c-43ec-a3ae-95b71a0e6068', 'default', 'office:document:delete', '文档删除', 'office', '2026-07-13 17:44:14.917673+08', '2026-07-13 17:44:14.917673+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('0bc32e28-e7a0-479c-a275-082dcd30d718', 'default', 'risk:view', '风险中心查看', 'risk', '2026-07-13 17:44:14.962253+08', '2026-07-13 17:44:14.962253+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('4183399a-3741-448c-a7da-7d1e0490356f', 'default', 'risk:update', '风险处理', 'risk', '2026-07-13 17:44:14.964752+08', '2026-07-13 17:44:14.964752+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('41105f9f-8e3d-497a-afad-dfc3ed252d0e', 'default', 'hr:employee:view', '员工查看', 'hr', '2026-07-13 17:44:14.967281+08', '2026-07-13 17:44:14.967281+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('cf95adc7-b21f-4185-a756-7bbceeb42c2c', 'default', 'hr:employee:manage', '员工管理', 'hr', '2026-07-13 17:44:14.969007+08', '2026-07-13 17:44:14.969007+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('ba2f3ac3-6f04-4e3f-8fe9-85dcc63b1ca7', 'default', 'maintenance:plan:manage', '维护计划管理', 'maintenance', '2026-07-13 17:44:14.972194+08', '2026-07-13 17:44:14.972194+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('cacf1aa1-a1ec-4a7e-b709-1d0a02ac9504', 'default', 'maintenance:order:manage', '工单管理', 'maintenance', '2026-07-13 17:44:14.97398+08', '2026-07-13 17:44:14.97398+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('3b3fb523-de9c-42a5-b574-443d59a3b928', 'default', 'maintenance:order:delete', '工单删除', 'maintenance', '2026-07-13 17:44:14.976753+08', '2026-07-13 17:44:14.976753+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('31093a82-6079-456a-9b9b-43a0220277c9', 'default', 'maintenance:certificate:view', '人员证书查看', 'maintenance', '2026-07-13 17:44:14.978503+08', '2026-07-13 17:44:14.978503+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('f5b60052-edec-4d8f-86f8-42f2a7c85ba1', 'default', 'maintenance:schedule:view', '人员排班查看', 'maintenance', '2026-07-13 17:44:14.979856+08', '2026-07-13 17:44:14.979856+08', NULL, NULL, false, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002506', 'default', 'crm:quote:cost', '报价成本询价', 'crm', '2026-07-15 17:45:26.214463+08', '2026-07-15 17:45:26.214463+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001001', 'default', 'dashboard:view', '经营驾驶舱查看', 'dashboard', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:54.938609+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001101', 'default', 'crm:customer:view', '客户池查看', 'crm', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:54.938609+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001102', 'default', 'crm:customer:create', '客户新增', 'crm', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:54.938609+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001201', 'default', 'crm:opportunity:view', '线索商机查看', 'crm', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:54.938609+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001401', 'default', 'procurement:view', '供应链采购查看', 'procurement', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:54.938609+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001501', 'default', 'project:view', '项目管理查看', 'project', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:54.938609+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001701', 'default', 'finance:view', '财务资金查看', 'finance', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:54.938609+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001801', 'default', 'system:view', '系统设置查看', 'system', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:54.938609+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('9810638a-835c-439c-ab37-860c9854f4de', 'default', 'project:delete', '项目删除', 'project', '2026-07-15 17:40:36.63465+08', '2026-07-24 13:53:14.655232+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002902', 'default', 'office:approval:view', '业务待办审批查看', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-24 13:54:15.030565+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001402', 'default', 'procurement:supplier:create', '供应商新增', 'procurement', '2026-07-02 11:05:54.969791+08', '2026-07-02 11:05:54.969791+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001403', 'default', 'procurement:purchase:create', '采购单据新增', 'procurement', '2026-07-02 11:05:54.969791+08', '2026-07-02 11:05:54.969791+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001502', 'default', 'project:create', '项目新增', 'project', '2026-07-02 11:05:54.969791+08', '2026-07-02 11:05:54.969791+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001603', 'default', 'inventory:movement:create', '库存流水新增', 'inventory', '2026-07-02 11:05:54.969791+08', '2026-07-02 11:05:54.969791+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001901', 'default', 'system:user:view', '员工管理查看', 'system', '2026-07-02 11:05:54.974947+08', '2026-07-02 11:05:54.974947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001902', 'default', 'system:user:create', '员工新增', 'system', '2026-07-02 11:05:54.974947+08', '2026-07-02 11:05:54.974947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002813', 'default', 'workforce:certificate:create', '人员证书新增', 'workforce', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.085154+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002901', 'default', 'office:view', 'OA协同查看', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002903', 'default', 'office:approval:create', '审批申请新增', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002904', 'default', 'office:approval:process', '审批处理', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002905', 'default', 'office:expense:view', '费用报销查看', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002906', 'default', 'office:expense:create', '费用报销新增', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002907', 'default', 'office:outsource:view', '外包服务查看', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001903', 'default', 'system:user:update', '员工编辑', 'system', '2026-07-02 11:05:54.974947+08', '2026-07-02 11:05:54.974947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001904', 'default', 'system:user:delete', '员工删除', 'system', '2026-07-02 11:05:54.974947+08', '2026-07-02 11:05:54.974947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001905', 'default', 'system:user:reset-password', '员工密码重置', 'system', '2026-07-02 11:05:54.974947+08', '2026-07-02 11:05:54.974947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002001', 'default', 'system:role:view', '角色管理查看', 'system', '2026-07-02 11:05:54.974947+08', '2026-07-02 11:05:54.974947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002002', 'default', 'system:role:create', '角色新增', 'system', '2026-07-02 11:05:54.974947+08', '2026-07-02 11:05:54.974947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002003', 'default', 'system:role:update', '角色编辑', 'system', '2026-07-02 11:05:54.974947+08', '2026-07-02 11:05:54.974947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002004', 'default', 'system:role:delete', '角色删除', 'system', '2026-07-02 11:05:54.974947+08', '2026-07-02 11:05:54.974947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002101', 'default', 'system:permission:view', '权限管理查看', 'system', '2026-07-02 11:05:54.974947+08', '2026-07-02 11:05:54.974947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002102', 'default', 'system:permission:create', '权限新增', 'system', '2026-07-02 11:05:54.979658+08', '2026-07-02 11:05:54.979658+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002103', 'default', 'system:permission:update', '权限编辑', 'system', '2026-07-02 11:05:54.979658+08', '2026-07-02 11:05:54.979658+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002104', 'default', 'system:permission:delete', '权限删除', 'system', '2026-07-02 11:05:54.979658+08', '2026-07-02 11:05:54.979658+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002201', 'default', 'system:organization:view', '组织架构查看', 'system', '2026-07-02 11:05:54.979658+08', '2026-07-02 11:05:54.979658+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002202', 'default', 'system:organization:create', '组织新增', 'system', '2026-07-02 11:05:54.979658+08', '2026-07-02 11:05:54.979658+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002203', 'default', 'system:organization:update', '组织编辑', 'system', '2026-07-02 11:05:54.979658+08', '2026-07-02 11:05:54.979658+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002204', 'default', 'system:organization:delete', '组织删除', 'system', '2026-07-02 11:05:54.979658+08', '2026-07-02 11:05:54.979658+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002301', 'default', 'crm:opportunity:create', '商机新增', 'crm', '2026-07-02 11:05:54.984702+08', '2026-07-02 11:05:54.984702+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002302', 'default', 'crm:opportunity:update', '商机推进', 'crm', '2026-07-02 11:05:54.984702+08', '2026-07-02 11:05:54.984702+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002303', 'default', 'crm:quote:view', '报价方案查看', 'crm', '2026-07-02 11:05:54.984702+08', '2026-07-02 11:05:54.984702+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002304', 'default', 'crm:quote:create', '报价方案新增', 'crm', '2026-07-02 11:05:54.984702+08', '2026-07-02 11:05:54.984702+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002305', 'default', 'crm:quote:submit', '报价提交审批', 'crm', '2026-07-02 11:05:54.984702+08', '2026-07-02 11:05:54.984702+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002306', 'default', 'crm:followup:view', '跟进回访查看', 'crm', '2026-07-02 11:05:54.984702+08', '2026-07-02 11:05:54.984702+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002307', 'default', 'crm:followup:create', '跟进回访新增', 'crm', '2026-07-02 11:05:54.984702+08', '2026-07-02 11:05:54.984702+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002308', 'default', 'crm:renewal:view', '续约管理查看', 'crm', '2026-07-02 11:05:54.984702+08', '2026-07-02 11:05:54.984702+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002309', 'default', 'crm:receivable:view', '合同应收查看', 'crm', '2026-07-02 11:05:54.984702+08', '2026-07-02 11:05:54.984702+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002310', 'default', 'crm:profile:view', '客户经营画像查看', 'crm', '2026-07-02 11:05:54.984702+08', '2026-07-02 11:05:54.984702+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002311', 'default', 'crm:quote:approve', '报价审批处理', 'crm', '2026-07-02 11:05:54.999252+08', '2026-07-02 11:05:54.999252+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002312', 'default', 'crm:receivable:invoice', '应收开票登记', 'crm', '2026-07-02 11:05:54.999252+08', '2026-07-02 11:05:54.999252+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002313', 'default', 'crm:receivable:settle', '应收回款登记', 'crm', '2026-07-02 11:05:54.999252+08', '2026-07-02 11:05:54.999252+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002401', 'default', 'procurement:request:approve', '采购申请审批', 'procurement', '2026-07-02 11:05:55.014247+08', '2026-07-02 11:05:55.014247+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002402', 'default', 'procurement:order:receive', '采购订单收货', 'procurement', '2026-07-02 11:05:55.014247+08', '2026-07-02 11:05:55.014247+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002403', 'default', 'procurement:payable:view', '采购应付查看', 'procurement', '2026-07-02 11:05:55.014247+08', '2026-07-02 11:05:55.014247+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002501', 'default', 'project:approve', '项目立项审批', 'project', '2026-07-02 11:05:55.031659+08', '2026-07-02 11:05:55.031659+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002502', 'default', 'project:stage:update', '项目阶段推进', 'project', '2026-07-02 11:05:55.031659+08', '2026-07-02 11:05:55.031659+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002503', 'default', 'project:cost:create', '项目成本归集', 'project', '2026-07-02 11:05:55.031659+08', '2026-07-02 11:05:55.031659+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002601', 'default', 'inventory:issue:create', '项目领料办理', 'inventory', '2026-07-02 11:05:55.051483+08', '2026-07-02 11:05:55.051483+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002602', 'default', 'inventory:return:create', '项目退料办理', 'inventory', '2026-07-02 11:05:55.051483+08', '2026-07-02 11:05:55.051483+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002701', 'default', 'finance:receivable:view', '财务应收查看', 'finance', '2026-07-02 11:05:55.069679+08', '2026-07-02 11:05:55.069679+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002702', 'default', 'finance:receivable:invoice', '财务开票登记', 'finance', '2026-07-02 11:05:55.069679+08', '2026-07-02 11:05:55.069679+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002703', 'default', 'finance:receivable:collect', '财务回款核销', 'finance', '2026-07-02 11:05:55.069679+08', '2026-07-02 11:05:55.069679+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002704', 'default', 'finance:payable:view', '财务应付查看', 'finance', '2026-07-02 11:05:55.069679+08', '2026-07-02 11:05:55.069679+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002705', 'default', 'finance:payment:apply', '付款申请新增', 'finance', '2026-07-02 11:05:55.069679+08', '2026-07-02 11:05:55.069679+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002706', 'default', 'finance:payment:approve', '付款申请审批', 'finance', '2026-07-02 11:05:55.069679+08', '2026-07-02 11:05:55.069679+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002707', 'default', 'finance:payment:execute', '财务付款执行', 'finance', '2026-07-02 11:05:55.069679+08', '2026-07-02 11:05:55.069679+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002908', 'default', 'office:outsource:create', '外包服务新增', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002909', 'default', 'office:outsource:complete', '外包服务验收', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002910', 'default', 'office:document:view', '电子档案查看', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002911', 'default', 'office:document:upload', '电子档案上传', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002912', 'default', 'office:notification:view', '消息中心查看', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002913', 'default', 'office:audit:view', '操作审计查看', 'office', '2026-07-02 11:05:55.125979+08', '2026-07-02 11:05:55.125979+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000003001', 'default', 'finance:ledger:view', '财务总账查看', 'finance', '2026-07-02 11:05:55.151002+08', '2026-07-02 11:05:55.151002+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000003101', 'default', 'bi:view', '经营分析查看', 'bi', '2026-07-02 11:05:55.162749+08', '2026-07-02 11:05:55.162749+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001301', 'default', 'crm:contract:view', '客户合同查看', 'crm', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001601', 'default', 'inventory:view', '库存管理查看', 'inventory', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000001602', 'default', 'inventory:part:create', '物料新增', 'inventory', '2026-07-02 11:05:54.969791+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002801', 'default', 'maintenance:view', '服务管理查看', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002802', 'default', 'maintenance:equipment:view', '资产设备查看', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002803', 'default', 'maintenance:equipment:create', '资产设备新增', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002804', 'default', 'maintenance:plan:view', '服务计划查看', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002805', 'default', 'maintenance:plan:create', '服务计划新增', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002806', 'default', 'maintenance:plan:generate', '服务工单生成', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002807', 'default', 'maintenance:workorder:view', '服务工单查看', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002808', 'default', 'maintenance:workorder:create', '服务工单新增', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002809', 'default', 'maintenance:workorder:assign', '服务工单派工', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002810', 'default', 'maintenance:workorder:execute', '服务工单执行', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002811', 'default', 'maintenance:workorder:accept', '服务工单验收', 'maintenance', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002812', 'default', 'workforce:view', '人事排班查看', 'workforce', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002814', 'default', 'workforce:schedule:create', '排班新增', 'workforce', '2026-07-02 11:05:55.085154+08', '2026-07-02 11:05:55.168016+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004101', 'default', 'qualification:view', '资质管理查看', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004102', 'default', 'qualification:company:view', '公司资质查看', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004103', 'default', 'qualification:company:manage', '公司资质维护', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004104', 'default', 'qualification:employee:view', '资质人员查看', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004105', 'default', 'qualification:employee:manage', '资质人员维护', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004106', 'default', 'qualification:certificate:view', '人员证书查看', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004107', 'default', 'qualification:certificate:manage', '人员证书维护', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004108', 'default', 'qualification:performance:view', '项目业绩查看', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004109', 'default', 'qualification:performance:manage', '项目业绩维护', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004110', 'default', 'qualification:tender:view', '投标查询查看', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000004111', 'default', 'qualification:warning:view', '资质预警查看', 'qualification', '2026-07-02 11:05:55.17742+08', '2026-07-02 11:05:55.17742+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002314', 'default', 'crm:customer:update', '客户档案编辑', 'crm', '2026-07-02 11:05:55.212593+08', '2026-07-02 11:05:55.212593+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002315', 'default', 'crm:quote:update', '报价方案修订', 'crm', '2026-07-02 11:05:55.216326+08', '2026-07-02 11:05:55.216326+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002316', 'default', 'crm:quote:customer-result', '报价客户结果登记', 'crm', '2026-07-02 11:05:55.216326+08', '2026-07-02 11:05:55.216326+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002317', 'default', 'crm:quote:convert', '报价转合同', 'crm', '2026-07-02 11:05:55.216326+08', '2026-07-02 11:05:55.216326+08', NULL, NULL, true, 0);
INSERT INTO public.sys_permissions (id, tenant_id, code, name, module, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000002105', 'default', 'system:deleted-records:manage', '删除回收站管理', 'system', '2026-07-15 17:45:26.14937+08', '2026-07-15 17:45:26.14937+08', NULL, NULL, true, 0);


--
-- Data for Name: sys_role_permissions; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001301');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001102');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001201');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001701');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001401');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001501');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001801');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000001101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000001102');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000001201');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000001301');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001603');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001602');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001403');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001402');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001502');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002002');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002004');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002003');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001905');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000001901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002202');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002204');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002203');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002201');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002102');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002104');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002103');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002307');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002306');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002301');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002302');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002310');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002304');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002305');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002303');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002309');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002308');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002307');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002306');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002301');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002302');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002310');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002304');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002305');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002303');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002309');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002308');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002311');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002311');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002312');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002313');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002402');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002403');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002401');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002501');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002503');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002502');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002602');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002704');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002705');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002706');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002707');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002703');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002702');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002701');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002803');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002802');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002805');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002806');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002804');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002801');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002811');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002809');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002808');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002810');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002807');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002813');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002814');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002812');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002913');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002911');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002910');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002905');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002909');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002908');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002907');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000003001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000003101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004107');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004106');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004103');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004102');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004105');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004104');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004109');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004108');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004110');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000004111');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002314');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002314');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002317');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002316');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002315');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002317');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002316');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000002315');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '918f5f25-cf44-4c07-b151-a55ec1afa36e');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '41105f9f-8e3d-497a-afad-dfc3ed252d0e');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '0bc32e28-e7a0-479c-a275-082dcd30d718');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', 'cacf1aa1-a1ec-4a7e-b709-1d0a02ac9504');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', 'acfe4c56-d46a-4e35-a79c-6f10e0511a3f');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', 'f571c12c-19dd-48f3-b6bc-4537957ff9a1');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', 'ba2f3ac3-6f04-4e3f-8fe9-85dcc63b1ca7');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '7f2113e8-ca5c-43ec-a3ae-95b71a0e6068');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', 'bf49da93-92c9-4145-ac19-a30e696db85e');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', 'f5b60052-edec-4d8f-86f8-42f2a7c85ba1');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '79f5ee96-a718-4957-8ee2-539b4ab2ca2d');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', 'c9e3bb51-ee48-4749-86d7-5ef30c91eba7');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '4183399a-3741-448c-a7da-7d1e0490356f');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '3b3fb523-de9c-42a5-b574-443d59a3b928');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', 'fe59ff2d-b3f7-4194-841b-5a845a828c39');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', 'cf95adc7-b21f-4185-a756-7bbceeb42c2c');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '6b43ebdb-2c94-464b-b9b2-db8f396a6286');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '717ae4d6-3473-4b54-9f0a-84d4c25bb187');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '31093a82-6079-456a-9b9b-43a0220277c9');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002308');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002701');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000003001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002303');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002309');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000001501');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000001601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002704');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000001201');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '918f5f25-cf44-4c07-b151-a55ec1afa36e');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002910');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000004111');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000001101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000001301');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002801');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000001401');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002403');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '0bc32e28-e7a0-479c-a275-082dcd30d718');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002201');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002913');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000004101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000001701');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', 'f571c12c-19dd-48f3-b6bc-4537957ff9a1');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002308');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002305');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002303');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000001102');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002309');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002304');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '717ae4d6-3473-4b54-9f0a-84d4c25bb187');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', 'fe59ff2d-b3f7-4194-841b-5a845a828c39');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002310');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000001201');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '918f5f25-cf44-4c07-b151-a55ec1afa36e');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002301');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002316');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002306');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000001101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', 'c9e3bb51-ee48-4749-86d7-5ef30c91eba7');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000001301');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002314');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', 'bf49da93-92c9-4145-ac19-a30e696db85e');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002317');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', 'acfe4c56-d46a-4e35-a79c-6f10e0511a3f');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002302');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002311');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002307');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '6b43ebdb-2c94-464b-b9b2-db8f396a6286');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', '00000000-0000-4000-8000-000000002315');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000001301');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002308');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002314');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002305');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002303');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000001102');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002309');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002302');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002304');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002307');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002310');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000001201');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '918f5f25-cf44-4c07-b151-a55ec1afa36e');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002301');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002316');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002306');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002315');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000001101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002701');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002403');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000003001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '0bc32e28-e7a0-479c-a275-082dcd30d718');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002703');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002707');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002309');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002705');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002704');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002706');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002702');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002312');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002313');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000002905');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '79f5ee96-a718-4957-8ee2-539b4ab2ca2d');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', '00000000-0000-4000-8000-000000001701');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002701');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002403');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000003001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002309');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002705');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002704');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002702');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002312');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002905');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000001701');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', '00000000-0000-4000-8000-000000002701');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', '00000000-0000-4000-8000-000000002704');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', '00000000-0000-4000-8000-000000003001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', '00000000-0000-4000-8000-000000002703');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', '00000000-0000-4000-8000-000000002707');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', '00000000-0000-4000-8000-000000001701');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004107');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002201');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004106');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004105');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', 'cf95adc7-b21f-4185-a756-7bbceeb42c2c');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002913');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002907');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002909');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004104');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002910');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002812');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '41105f9f-8e3d-497a-afad-dfc3ed252d0e');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002908');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002905');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002911');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000001901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004107');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004106');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004105');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', 'cf95adc7-b21f-4185-a756-7bbceeb42c2c');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000002907');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004104');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000002910');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000002812');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '41105f9f-8e3d-497a-afad-dfc3ed252d0e');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000002908');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000002905');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000002911');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000001901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000002401');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000001401');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000002403');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '0bc32e28-e7a0-479c-a275-082dcd30d718');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000002402');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000001603');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000001403');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000001601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000002904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000001402');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', '00000000-0000-4000-8000-000000001602');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000001401');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000002403');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000001402');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000002402');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000001403');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000001601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000001401');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '0bc32e28-e7a0-479c-a275-082dcd30d718');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000001501');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000001601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002602');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002501');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000001502');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('ff312c0b-3b5b-42a0-a1ad-4acbb0d2362c', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('ff312c0b-3b5b-42a0-a1ad-4acbb0d2362c', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('ff312c0b-3b5b-42a0-a1ad-4acbb0d2362c', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('ff312c0b-3b5b-42a0-a1ad-4acbb0d2362c', '00000000-0000-4000-8000-000000001501');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('ff312c0b-3b5b-42a0-a1ad-4acbb0d2362c', '00000000-0000-4000-8000-000000002601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('ff312c0b-3b5b-42a0-a1ad-4acbb0d2362c', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('ff312c0b-3b5b-42a0-a1ad-4acbb0d2362c', '00000000-0000-4000-8000-000000001601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('ff312c0b-3b5b-42a0-a1ad-4acbb0d2362c', '00000000-0000-4000-8000-000000002602');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('ff312c0b-3b5b-42a0-a1ad-4acbb0d2362c', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000002801');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '31093a82-6079-456a-9b9b-43a0220277c9');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '0bc32e28-e7a0-479c-a275-082dcd30d718');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '3b3fb523-de9c-42a5-b574-443d59a3b928');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000001501');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', 'ba2f3ac3-6f04-4e3f-8fe9-85dcc63b1ca7');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000001601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000002602');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', 'cacf1aa1-a1ec-4a7e-b709-1d0a02ac9504');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000002904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000002812');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', '00000000-0000-4000-8000-000000002601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', 'f5b60052-edec-4d8f-86f8-42f2a7c85ba1');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', '00000000-0000-4000-8000-000000002801');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', 'cacf1aa1-a1ec-4a7e-b709-1d0a02ac9504');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', '31093a82-6079-456a-9b9b-43a0220277c9');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', '00000000-0000-4000-8000-000000002601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', 'f5b60052-edec-4d8f-86f8-42f2a7c85ba1');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', '00000000-0000-4000-8000-000000001601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000002801');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '31093a82-6079-456a-9b9b-43a0220277c9');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000004102');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000001501');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', 'ba2f3ac3-6f04-4e3f-8fe9-85dcc63b1ca7');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000004110');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000002904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000004111');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', '00000000-0000-4000-8000-000000004101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', 'f5b60052-edec-4d8f-86f8-42f2a7c85ba1');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000002201');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000001801');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000002101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000002913');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000002001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000002003');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000001903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000001902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000001901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000002202');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000001905');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', '00000000-0000-4000-8000-000000002203');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000004107');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000004102');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000002902');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000004103');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000004106');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000004105');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000004110');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000002904');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000004104');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000004111');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000002910');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000004101');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', '00000000-0000-4000-8000-000000002911');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('537ec1d5-0ade-41a5-b2ee-94b394873ac6', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('537ec1d5-0ade-41a5-b2ee-94b394873ac6', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('537ec1d5-0ade-41a5-b2ee-94b394873ac6', '00000000-0000-4000-8000-000000001001');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('537ec1d5-0ade-41a5-b2ee-94b394873ac6', '00000000-0000-4000-8000-000000002912');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('537ec1d5-0ade-41a5-b2ee-94b394873ac6', '00000000-0000-4000-8000-000000002901');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '9810638a-835c-439c-ab37-860c9854f4de');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002105');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000002506');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002313');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002703');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002311');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002501');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002506');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002317');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002707');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', '00000000-0000-4000-8000-000000002706');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', '00000000-0000-4000-8000-000000002317');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002502');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002506');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', '00000000-0000-4000-8000-000000002503');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000001602');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000002602');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000001603');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', '00000000-0000-4000-8000-000000002601');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002706');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', '00000000-0000-4000-8000-000000002707');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002906');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000002903');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004108');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004110');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004103');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004102');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004111');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', '00000000-0000-4000-8000-000000004109');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004102');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004103');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004110');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004111');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004108');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', '00000000-0000-4000-8000-000000004109');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', 'fe59ff2d-b3f7-4194-841b-5a845a828c39');
INSERT INTO public.sys_role_permissions (role_id, permission_id) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', 'fe59ff2d-b3f7-4194-841b-5a845a828c39');


--
-- Data for Name: sys_roles; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000000101', 'default', 'ADMIN', '系统管理员', 'ALL', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:54.938609+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('00000000-0000-4000-8000-000000000102', 'default', 'CRM_MANAGER', '客户经理', 'DEPARTMENT', '2026-07-02 11:05:54.938609+08', '2026-07-02 11:05:54.938609+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('6a98e6aa-102f-4c92-b7fa-97cc4e7aaaac', 'default', 'EXECUTIVE_MANAGER', '总经办负责人', 'ALL', '2026-07-14 13:41:07.916029+08', '2026-07-14 13:41:07.916029+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('4f83be48-c31b-4f46-9190-f3d23735ddd7', 'default', 'SALES_DIRECTOR', '市场销售负责人', 'DEPT_AND_SUB', '2026-07-14 13:41:07.926607+08', '2026-07-14 13:41:07.926607+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('adb359a5-cdfe-41f0-95f1-33eefbc3eb4c', 'default', 'SALES_REP', '销售专员', 'SELF', '2026-07-14 13:41:07.934452+08', '2026-07-14 13:41:07.934452+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('2bc1f2d2-032c-4a0a-9c26-267a4ac18619', 'default', 'FINANCE_MANAGER', '财务负责人', 'ALL', '2026-07-14 13:41:07.94187+08', '2026-07-14 13:41:07.94187+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('3a77aa44-3eaa-4d8e-9b8c-33065666db69', 'default', 'FINANCE_ACCOUNTANT', '会计', 'DEPT_AND_SUB', '2026-07-14 13:41:07.947183+08', '2026-07-14 13:41:07.947183+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('8b70e60c-baa5-4462-a4ab-8e051641df4f', 'default', 'FINANCE_CASHIER', '出纳', 'SELF', '2026-07-14 13:41:07.951167+08', '2026-07-14 13:41:07.951167+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('1502c1a4-3bbd-4a8d-9c2c-1a1f816a3d07', 'default', 'HR_MANAGER', '人事行政负责人', 'ALL', '2026-07-14 13:41:07.955224+08', '2026-07-14 13:41:07.955224+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('b8abd439-b3d2-4694-81ca-1897bd4caf4e', 'default', 'HR_SPECIALIST', '人事专员', 'DEPT_AND_SUB', '2026-07-14 13:41:07.961324+08', '2026-07-14 13:41:07.961324+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('5ea7f9f8-3a97-4647-a9b1-4724bfc60c92', 'default', 'PROCUREMENT_MANAGER', '采购负责人', 'DEPT_AND_SUB', '2026-07-14 13:41:07.966982+08', '2026-07-14 13:41:07.966982+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('0414bd4a-d51e-40a4-8396-21b0714b06b9', 'default', 'PROCUREMENT_SPECIALIST', '采购专员', 'DEPT_AND_SUB', '2026-07-14 13:41:07.971847+08', '2026-07-14 13:41:07.971847+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('44cb1783-af4b-4c06-8f94-d4a4d1d29695', 'default', 'PROJECT_MANAGER', '项目经理', 'DEPT_AND_SUB', '2026-07-14 13:41:07.975628+08', '2026-07-14 13:41:07.975628+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('ff312c0b-3b5b-42a0-a1ad-4acbb0d2362c', 'default', 'PROJECT_MEMBER', '项目成员', 'SELF', '2026-07-14 13:41:07.97947+08', '2026-07-14 13:41:07.97947+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('1f211b46-0d43-4641-895c-582b9c120d81', 'default', 'OPS_MANAGER', '运维负责人', 'DEPT_AND_SUB', '2026-07-14 13:41:07.98183+08', '2026-07-14 13:41:07.98183+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('4f9317f0-a583-4260-aec0-ad99e3595d88', 'default', 'OPS_ENGINEER', '运维工程师', 'SELF', '2026-07-14 13:41:07.986145+08', '2026-07-14 13:41:07.986145+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('33849752-0f3d-4654-aed9-9017caac7433', 'default', 'TECH_MANAGER', '技术负责人', 'DEPT_AND_SUB', '2026-07-14 13:41:07.989271+08', '2026-07-14 13:41:07.989271+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('f6786e09-cd4c-47d0-9db9-678d88d379d7', 'default', 'SYSTEM_OPERATOR', '系统运维', 'ALL', '2026-07-14 13:41:07.993346+08', '2026-07-14 13:41:07.993346+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('135a2cc2-146f-4f9b-9cd7-753489c1778c', 'default', 'QUALIFICATION_MANAGER', '资质负责人', 'ALL', '2026-07-14 13:41:07.997046+08', '2026-07-14 13:41:07.997046+08', NULL, NULL, true, 0);
INSERT INTO public.sys_roles (id, tenant_id, code, name, data_scope, created_at, updated_at, created_by, updated_by, built_in, version) VALUES ('537ec1d5-0ade-41a5-b2ee-94b394873ac6', 'default', 'VIEWER', '普通员工', 'SELF', '2026-07-14 13:41:08.001313+08', '2026-07-14 13:41:08.001313+08', NULL, NULL, true, 0);


--
-- Name: approval_assignee_configs approval_assignee_configs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.approval_assignee_configs
    ADD CONSTRAINT approval_assignee_configs_pkey PRIMARY KEY (id);


--
-- Name: biz_collaboration_action_logs biz_collaboration_action_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_collaboration_action_logs
    ADD CONSTRAINT biz_collaboration_action_logs_pkey PRIMARY KEY (id);


--
-- Name: biz_collaboration_task_controls biz_collaboration_task_controls_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_collaboration_task_controls
    ADD CONSTRAINT biz_collaboration_task_controls_pkey PRIMARY KEY (id);


--
-- Name: biz_project_budget_versions biz_project_budget_versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_project_budget_versions
    ADD CONSTRAINT biz_project_budget_versions_pkey PRIMARY KEY (id);


--
-- Name: biz_project_handovers biz_project_handovers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_project_handovers
    ADD CONSTRAINT biz_project_handovers_pkey PRIMARY KEY (id);


--
-- Name: biz_project_staff_assignments biz_project_staff_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_project_staff_assignments
    ADD CONSTRAINT biz_project_staff_assignments_pkey PRIMARY KEY (id);


--
-- Name: biz_project_timesheets biz_project_timesheets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_project_timesheets
    ADD CONSTRAINT biz_project_timesheets_pkey PRIMARY KEY (id);


--
-- Name: biz_responsibility_bindings biz_responsibility_bindings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_responsibility_bindings
    ADD CONSTRAINT biz_responsibility_bindings_pkey PRIMARY KEY (id);


--
-- Name: biz_responsibility_collaborators biz_responsibility_collaborators_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_responsibility_collaborators
    ADD CONSTRAINT biz_responsibility_collaborators_pkey PRIMARY KEY (id);


--
-- Name: biz_timesheet_period_locks biz_timesheet_period_locks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_timesheet_period_locks
    ADD CONSTRAINT biz_timesheet_period_locks_pkey PRIMARY KEY (id);


--
-- Name: code_sequences code_sequences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.code_sequences
    ADD CONSTRAINT code_sequences_pkey PRIMARY KEY (entity_type);


--
-- Name: crm_attachment crm_attachment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_attachment
    ADD CONSTRAINT crm_attachment_pkey PRIMARY KEY (id);


--
-- Name: crm_contract_changes crm_contract_changes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_contract_changes
    ADD CONSTRAINT crm_contract_changes_pkey PRIMARY KEY (id);


--
-- Name: crm_customer_contacts crm_customer_contacts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_customer_contacts
    ADD CONSTRAINT crm_customer_contacts_pkey PRIMARY KEY (id);


--
-- Name: crm_customer_sites crm_customer_sites_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_customer_sites
    ADD CONSTRAINT crm_customer_sites_pkey PRIMARY KEY (id);


--
-- Name: crm_customers crm_customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_customers
    ADD CONSTRAINT crm_customers_pkey PRIMARY KEY (id);


--
-- Name: crm_customers crm_customers_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_customers
    ADD CONSTRAINT crm_customers_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: crm_follow_ups crm_follow_ups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_follow_ups
    ADD CONSTRAINT crm_follow_ups_pkey PRIMARY KEY (id);


--
-- Name: crm_opportunities crm_opportunities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_opportunities
    ADD CONSTRAINT crm_opportunities_pkey PRIMARY KEY (id);


--
-- Name: crm_opportunities crm_opportunities_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_opportunities
    ADD CONSTRAINT crm_opportunities_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: crm_quote_approval_records crm_quote_approval_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_approval_records
    ADD CONSTRAINT crm_quote_approval_records_pkey PRIMARY KEY (id);


--
-- Name: crm_quote_cost_requests crm_quote_cost_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_cost_requests
    ADD CONSTRAINT crm_quote_cost_requests_pkey PRIMARY KEY (id);


--
-- Name: crm_quote_plans crm_quote_plans_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_plans
    ADD CONSTRAINT crm_quote_plans_pkey PRIMARY KEY (id);


--
-- Name: crm_quote_plans crm_quote_plans_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_plans
    ADD CONSTRAINT crm_quote_plans_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: crm_quote_revisions crm_quote_revisions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_revisions
    ADD CONSTRAINT crm_quote_revisions_pkey PRIMARY KEY (id);


--
-- Name: crm_quote_revisions crm_quote_revisions_quote_id_version_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_revisions
    ADD CONSTRAINT crm_quote_revisions_quote_id_version_no_key UNIQUE (quote_id, version_no);


--
-- Name: crm_service_contracts crm_service_contracts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_service_contracts
    ADD CONSTRAINT crm_service_contracts_pkey PRIMARY KEY (id);


--
-- Name: crm_service_contracts crm_service_contracts_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_service_contracts
    ADD CONSTRAINT crm_service_contracts_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: doc_files doc_files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doc_files
    ADD CONSTRAINT doc_files_pkey PRIMARY KEY (id);


--
-- Name: fin_accounting_entries fin_accounting_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_accounting_entries
    ADD CONSTRAINT fin_accounting_entries_pkey PRIMARY KEY (id);


--
-- Name: fin_accounting_vouchers fin_accounting_vouchers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_accounting_vouchers
    ADD CONSTRAINT fin_accounting_vouchers_pkey PRIMARY KEY (id);


--
-- Name: fin_accounting_vouchers fin_accounting_vouchers_tenant_id_biz_type_biz_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_accounting_vouchers
    ADD CONSTRAINT fin_accounting_vouchers_tenant_id_biz_type_biz_no_key UNIQUE (tenant_id, biz_type, biz_no);


--
-- Name: fin_accounting_vouchers fin_accounting_vouchers_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_accounting_vouchers
    ADD CONSTRAINT fin_accounting_vouchers_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: fin_payment_applications fin_payment_applications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_applications
    ADD CONSTRAINT fin_payment_applications_pkey PRIMARY KEY (id);


--
-- Name: fin_payment_applications fin_payment_applications_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_applications
    ADD CONSTRAINT fin_payment_applications_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: fin_payment_records fin_payment_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_records
    ADD CONSTRAINT fin_payment_records_pkey PRIMARY KEY (id);


--
-- Name: fin_payment_records fin_payment_records_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_records
    ADD CONSTRAINT fin_payment_records_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: fin_procurement_payables fin_procurement_payables_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_procurement_payables
    ADD CONSTRAINT fin_procurement_payables_pkey PRIMARY KEY (id);


--
-- Name: fin_procurement_payables fin_procurement_payables_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_procurement_payables
    ADD CONSTRAINT fin_procurement_payables_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: fin_receivable_receipts fin_receivable_receipts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_receivable_receipts
    ADD CONSTRAINT fin_receivable_receipts_pkey PRIMARY KEY (id);


--
-- Name: fin_receivables fin_receivables_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_receivables
    ADD CONSTRAINT fin_receivables_pkey PRIMARY KEY (id);


--
-- Name: fin_receivables fin_receivables_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_receivables
    ADD CONSTRAINT fin_receivables_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: hr_emergency_contacts hr_emergency_contacts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_emergency_contacts
    ADD CONSTRAINT hr_emergency_contacts_pkey PRIMARY KEY (id);


--
-- Name: hr_employee_certificates hr_employee_certificates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_certificates
    ADD CONSTRAINT hr_employee_certificates_pkey PRIMARY KEY (id);


--
-- Name: hr_employee_certificates hr_employee_certificates_tenant_id_certificate_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_certificates
    ADD CONSTRAINT hr_employee_certificates_tenant_id_certificate_no_key UNIQUE (tenant_id, certificate_no);


--
-- Name: hr_employee_contracts hr_employee_contracts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_contracts
    ADD CONSTRAINT hr_employee_contracts_pkey PRIMARY KEY (id);


--
-- Name: hr_employee_contracts hr_employee_contracts_tenant_id_contract_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_contracts
    ADD CONSTRAINT hr_employee_contracts_tenant_id_contract_no_key UNIQUE (tenant_id, contract_no);


--
-- Name: hr_employee_education hr_employee_education_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_education
    ADD CONSTRAINT hr_employee_education_pkey PRIMARY KEY (id);


--
-- Name: hr_employee_lifecycle_records hr_employee_lifecycle_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_lifecycle_records
    ADD CONSTRAINT hr_employee_lifecycle_records_pkey PRIMARY KEY (id);


--
-- Name: hr_employee_work_experience hr_employee_work_experience_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_work_experience
    ADD CONSTRAINT hr_employee_work_experience_pkey PRIMARY KEY (id);


--
-- Name: hr_field_attendance hr_field_attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_field_attendance
    ADD CONSTRAINT hr_field_attendance_pkey PRIMARY KEY (id);


--
-- Name: hr_field_schedules hr_field_schedules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_field_schedules
    ADD CONSTRAINT hr_field_schedules_pkey PRIMARY KEY (id);


--
-- Name: hr_leave_balances hr_leave_balances_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_leave_balances
    ADD CONSTRAINT hr_leave_balances_pkey PRIMARY KEY (id);


--
-- Name: hr_leave_requests hr_leave_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_leave_requests
    ADD CONSTRAINT hr_leave_requests_pkey PRIMARY KEY (id);


--
-- Name: inventory_issue_lines inventory_issue_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_issue_lines
    ADD CONSTRAINT inventory_issue_lines_pkey PRIMARY KEY (id);


--
-- Name: inventory_issue_orders inventory_issue_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_issue_orders
    ADD CONSTRAINT inventory_issue_orders_pkey PRIMARY KEY (id);


--
-- Name: inventory_issue_orders inventory_issue_orders_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_issue_orders
    ADD CONSTRAINT inventory_issue_orders_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: inventory_parts inventory_parts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_parts
    ADD CONSTRAINT inventory_parts_pkey PRIMARY KEY (id);


--
-- Name: inventory_parts inventory_parts_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_parts
    ADD CONSTRAINT inventory_parts_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: inventory_return_lines inventory_return_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_return_lines
    ADD CONSTRAINT inventory_return_lines_pkey PRIMARY KEY (id);


--
-- Name: inventory_return_orders inventory_return_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_return_orders
    ADD CONSTRAINT inventory_return_orders_pkey PRIMARY KEY (id);


--
-- Name: inventory_return_orders inventory_return_orders_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_return_orders
    ADD CONSTRAINT inventory_return_orders_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: inventory_stock_movements inventory_stock_movements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_stock_movements
    ADD CONSTRAINT inventory_stock_movements_pkey PRIMARY KEY (id);


--
-- Name: maintenance_equipment_assets maintenance_equipment_assets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_equipment_assets
    ADD CONSTRAINT maintenance_equipment_assets_pkey PRIMARY KEY (id);


--
-- Name: maintenance_equipment_assets maintenance_equipment_assets_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_equipment_assets
    ADD CONSTRAINT maintenance_equipment_assets_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: maintenance_plans maintenance_plans_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_plans
    ADD CONSTRAINT maintenance_plans_pkey PRIMARY KEY (id);


--
-- Name: maintenance_plans maintenance_plans_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_plans
    ADD CONSTRAINT maintenance_plans_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: oa_approval_actions oa_approval_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_approval_actions
    ADD CONSTRAINT oa_approval_actions_pkey PRIMARY KEY (id);


--
-- Name: oa_approval_requests oa_approval_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_approval_requests
    ADD CONSTRAINT oa_approval_requests_pkey PRIMARY KEY (id);


--
-- Name: oa_approval_requests oa_approval_requests_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_approval_requests
    ADD CONSTRAINT oa_approval_requests_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: oa_approval_runtime_nodes oa_approval_runtime_nodes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_approval_runtime_nodes
    ADD CONSTRAINT oa_approval_runtime_nodes_pkey PRIMARY KEY (id);


--
-- Name: oa_expense_claim_lines oa_expense_claim_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_expense_claim_lines
    ADD CONSTRAINT oa_expense_claim_lines_pkey PRIMARY KEY (id);


--
-- Name: oa_expense_claims oa_expense_claims_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_expense_claims
    ADD CONSTRAINT oa_expense_claims_pkey PRIMARY KEY (id);


--
-- Name: oa_expense_claims oa_expense_claims_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_expense_claims
    ADD CONSTRAINT oa_expense_claims_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: oa_outsource_orders oa_outsource_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_outsource_orders
    ADD CONSTRAINT oa_outsource_orders_pkey PRIMARY KEY (id);


--
-- Name: oa_outsource_orders oa_outsource_orders_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_outsource_orders
    ADD CONSTRAINT oa_outsource_orders_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: operation_audits operation_audits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.operation_audits
    ADD CONSTRAINT operation_audits_pkey PRIMARY KEY (id);


--
-- Name: procurement_action_logs procurement_action_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_action_logs
    ADD CONSTRAINT procurement_action_logs_pkey PRIMARY KEY (id);


--
-- Name: procurement_collaboration_events procurement_collaboration_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_collaboration_events
    ADD CONSTRAINT procurement_collaboration_events_pkey PRIMARY KEY (id);


--
-- Name: procurement_contracts procurement_contracts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_contracts
    ADD CONSTRAINT procurement_contracts_pkey PRIMARY KEY (id);


--
-- Name: procurement_cost_allocations procurement_cost_allocations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_cost_allocations
    ADD CONSTRAINT procurement_cost_allocations_pkey PRIMARY KEY (id);


--
-- Name: procurement_cost_allocations procurement_cost_allocations_receipt_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_cost_allocations
    ADD CONSTRAINT procurement_cost_allocations_receipt_id_key UNIQUE (receipt_id);


--
-- Name: procurement_goods_receipts procurement_goods_receipts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_goods_receipts
    ADD CONSTRAINT procurement_goods_receipts_pkey PRIMARY KEY (id);


--
-- Name: procurement_goods_receipts procurement_goods_receipts_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_goods_receipts
    ADD CONSTRAINT procurement_goods_receipts_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: procurement_inquiries procurement_inquiries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_inquiries
    ADD CONSTRAINT procurement_inquiries_pkey PRIMARY KEY (id);


--
-- Name: procurement_inquiry_requests procurement_inquiry_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_inquiry_requests
    ADD CONSTRAINT procurement_inquiry_requests_pkey PRIMARY KEY (id);


--
-- Name: procurement_purchase_orders procurement_purchase_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_orders
    ADD CONSTRAINT procurement_purchase_orders_pkey PRIMARY KEY (id);


--
-- Name: procurement_purchase_orders procurement_purchase_orders_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_orders
    ADD CONSTRAINT procurement_purchase_orders_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: procurement_purchase_requests procurement_purchase_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_requests
    ADD CONSTRAINT procurement_purchase_requests_pkey PRIMARY KEY (id);


--
-- Name: procurement_purchase_requests procurement_purchase_requests_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_requests
    ADD CONSTRAINT procurement_purchase_requests_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: procurement_request_approval_records procurement_request_approval_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_request_approval_records
    ADD CONSTRAINT procurement_request_approval_records_pkey PRIMARY KEY (id);


--
-- Name: procurement_return_orders procurement_return_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_return_orders
    ADD CONSTRAINT procurement_return_orders_pkey PRIMARY KEY (id);


--
-- Name: procurement_supplier_change_requests procurement_supplier_change_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_supplier_change_requests
    ADD CONSTRAINT procurement_supplier_change_requests_pkey PRIMARY KEY (id);


--
-- Name: procurement_supplier_invoices procurement_supplier_invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_supplier_invoices
    ADD CONSTRAINT procurement_supplier_invoices_pkey PRIMARY KEY (id);


--
-- Name: procurement_supplier_quote_lines procurement_supplier_quote_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_supplier_quote_lines
    ADD CONSTRAINT procurement_supplier_quote_lines_pkey PRIMARY KEY (id);


--
-- Name: procurement_supplier_quotes procurement_supplier_quotes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_supplier_quotes
    ADD CONSTRAINT procurement_supplier_quotes_pkey PRIMARY KEY (id);


--
-- Name: procurement_supplier_reviews procurement_supplier_reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_supplier_reviews
    ADD CONSTRAINT procurement_supplier_reviews_pkey PRIMARY KEY (id);


--
-- Name: procurement_suppliers procurement_suppliers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_suppliers
    ADD CONSTRAINT procurement_suppliers_pkey PRIMARY KEY (id);


--
-- Name: procurement_suppliers procurement_suppliers_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_suppliers
    ADD CONSTRAINT procurement_suppliers_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: project_budget_items project_budget_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_budget_items
    ADD CONSTRAINT project_budget_items_pkey PRIMARY KEY (id);


--
-- Name: project_cost_entries project_cost_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_cost_entries
    ADD CONSTRAINT project_cost_entries_pkey PRIMARY KEY (id);


--
-- Name: project_projects project_projects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_projects
    ADD CONSTRAINT project_projects_pkey PRIMARY KEY (id);


--
-- Name: project_projects project_projects_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_projects
    ADD CONSTRAINT project_projects_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: project_stage_records project_stage_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_stage_records
    ADD CONSTRAINT project_stage_records_pkey PRIMARY KEY (id);


--
-- Name: qual_company_qualifications qual_company_qualifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_company_qualifications
    ADD CONSTRAINT qual_company_qualifications_pkey PRIMARY KEY (id);


--
-- Name: qual_company_qualifications qual_company_qualifications_tenant_id_external_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_company_qualifications
    ADD CONSTRAINT qual_company_qualifications_tenant_id_external_id_key UNIQUE (tenant_id, external_id);


--
-- Name: qual_employees qual_employees_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_employees
    ADD CONSTRAINT qual_employees_pkey PRIMARY KEY (id);


--
-- Name: qual_employees qual_employees_tenant_id_external_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_employees
    ADD CONSTRAINT qual_employees_tenant_id_external_id_key UNIQUE (tenant_id, external_id);


--
-- Name: qual_performances qual_performances_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_performances
    ADD CONSTRAINT qual_performances_pkey PRIMARY KEY (id);


--
-- Name: qual_performances qual_performances_tenant_id_external_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_performances
    ADD CONSTRAINT qual_performances_tenant_id_external_id_key UNIQUE (tenant_id, external_id);


--
-- Name: qual_personnel_certificates qual_personnel_certificates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_personnel_certificates
    ADD CONSTRAINT qual_personnel_certificates_pkey PRIMARY KEY (id);


--
-- Name: qual_personnel_certificates qual_personnel_certificates_tenant_id_external_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_personnel_certificates
    ADD CONSTRAINT qual_personnel_certificates_tenant_id_external_id_key UNIQUE (tenant_id, external_id);


--
-- Name: risk_rule_configs risk_rule_configs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_rule_configs
    ADD CONSTRAINT risk_rule_configs_pkey PRIMARY KEY (id);


--
-- Name: risk_snapshots risk_snapshots_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_snapshots
    ADD CONSTRAINT risk_snapshots_pkey PRIMARY KEY (id);


--
-- Name: risk_workflow_actions risk_workflow_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_workflow_actions
    ADD CONSTRAINT risk_workflow_actions_pkey PRIMARY KEY (id);


--
-- Name: risk_workflows risk_workflows_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_workflows
    ADD CONSTRAINT risk_workflows_pkey PRIMARY KEY (id);


--
-- Name: shedlock shedlock_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shedlock
    ADD CONSTRAINT shedlock_pkey PRIMARY KEY (name);


--
-- Name: sys_organizations sys_organizations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_organizations
    ADD CONSTRAINT sys_organizations_pkey PRIMARY KEY (id);


--
-- Name: sys_organizations sys_organizations_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_organizations
    ADD CONSTRAINT sys_organizations_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: sys_permissions sys_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_permissions
    ADD CONSTRAINT sys_permissions_pkey PRIMARY KEY (id);


--
-- Name: sys_permissions sys_permissions_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_permissions
    ADD CONSTRAINT sys_permissions_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: sys_role_data_organizations sys_role_data_organizations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_data_organizations
    ADD CONSTRAINT sys_role_data_organizations_pkey PRIMARY KEY (role_id, organization_id);


--
-- Name: sys_role_permissions sys_role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_permissions
    ADD CONSTRAINT sys_role_permissions_pkey PRIMARY KEY (role_id, permission_id);


--
-- Name: sys_roles sys_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_roles
    ADD CONSTRAINT sys_roles_pkey PRIMARY KEY (id);


--
-- Name: sys_roles sys_roles_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_roles
    ADD CONSTRAINT sys_roles_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: sys_soft_delete_records sys_soft_delete_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_soft_delete_records
    ADD CONSTRAINT sys_soft_delete_records_pkey PRIMARY KEY (id);


--
-- Name: sys_user_roles sys_user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_roles
    ADD CONSTRAINT sys_user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: sys_users sys_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_users
    ADD CONSTRAINT sys_users_pkey PRIMARY KEY (id);


--
-- Name: sys_users sys_users_tenant_id_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_users
    ADD CONSTRAINT sys_users_tenant_id_username_key UNIQUE (tenant_id, username);


--
-- Name: system_audit_logs system_audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_audit_logs
    ADD CONSTRAINT system_audit_logs_pkey PRIMARY KEY (id);


--
-- Name: system_notifications system_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_notifications
    ADD CONSTRAINT system_notifications_pkey PRIMARY KEY (id);


--
-- Name: biz_collaboration_task_controls uk_collaboration_task_source; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_collaboration_task_controls
    ADD CONSTRAINT uk_collaboration_task_source UNIQUE (tenant_id, source_type, source_id);


--
-- Name: procurement_inquiry_requests uk_proc_inquiry_request_link; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_inquiry_requests
    ADD CONSTRAINT uk_proc_inquiry_request_link UNIQUE (tenant_id, inquiry_id, request_id);


--
-- Name: procurement_supplier_quote_lines uk_proc_quote_request_line; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_supplier_quote_lines
    ADD CONSTRAINT uk_proc_quote_request_line UNIQUE (tenant_id, quote_id, request_id);


--
-- Name: procurement_contracts uk_procurement_contract_no; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_contracts
    ADD CONSTRAINT uk_procurement_contract_no UNIQUE (tenant_id, contract_no, version_no);


--
-- Name: procurement_inquiries uk_procurement_inquiry_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_inquiries
    ADD CONSTRAINT uk_procurement_inquiry_code UNIQUE (tenant_id, code);


--
-- Name: procurement_supplier_invoices uk_procurement_invoice_no; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_supplier_invoices
    ADD CONSTRAINT uk_procurement_invoice_no UNIQUE (tenant_id, invoice_no);


--
-- Name: procurement_return_orders uk_procurement_return_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_return_orders
    ADD CONSTRAINT uk_procurement_return_code UNIQUE (tenant_id, code);


--
-- Name: biz_project_budget_versions uk_project_budget_version; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_project_budget_versions
    ADD CONSTRAINT uk_project_budget_version UNIQUE (tenant_id, project_id, version_no);


--
-- Name: biz_project_handovers uk_project_handover; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_project_handovers
    ADD CONSTRAINT uk_project_handover UNIQUE (tenant_id, project_id);


--
-- Name: biz_project_staff_assignments uk_project_staff; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_project_staff_assignments
    ADD CONSTRAINT uk_project_staff UNIQUE (tenant_id, project_id, user_id, role_name);


--
-- Name: biz_responsibility_collaborators uk_responsibility_collaborator; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_responsibility_collaborators
    ADD CONSTRAINT uk_responsibility_collaborator UNIQUE (tenant_id, binding_id, department_id);


--
-- Name: biz_responsibility_bindings uk_responsibility_source; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_responsibility_bindings
    ADD CONSTRAINT uk_responsibility_source UNIQUE (tenant_id, source_type, source_id);


--
-- Name: procurement_supplier_reviews uk_supplier_review_period; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_supplier_reviews
    ADD CONSTRAINT uk_supplier_review_period UNIQUE (tenant_id, supplier_id, review_period);


--
-- Name: biz_timesheet_period_locks uk_timesheet_period_lock; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_timesheet_period_locks
    ADD CONSTRAINT uk_timesheet_period_lock UNIQUE (tenant_id, year_month);


--
-- Name: risk_workflows uks225n9ue9mbt20qern0ny2ogh; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_workflows
    ADD CONSTRAINT uks225n9ue9mbt20qern0ny2ogh UNIQUE (risk_key);


--
-- Name: risk_rule_configs uks4w2e5f5yk5kaq5c8gmmj4t01; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_rule_configs
    ADD CONSTRAINT uks4w2e5f5yk5kaq5c8gmmj4t01 UNIQUE (rule_code);


--
-- Name: work_order_materials work_order_materials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_materials
    ADD CONSTRAINT work_order_materials_pkey PRIMARY KEY (id);


--
-- Name: work_order_status_logs work_order_status_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_status_logs
    ADD CONSTRAINT work_order_status_logs_pkey PRIMARY KEY (id);


--
-- Name: work_orders work_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_pkey PRIMARY KEY (id);


--
-- Name: work_orders work_orders_tenant_id_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_tenant_id_code_key UNIQUE (tenant_id, code);


--
-- Name: idx_approval_assignee_condition; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_approval_assignee_condition ON public.approval_assignee_configs USING btree (flow_code, condition_type, enabled);


--
-- Name: idx_approval_assignee_flow; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_approval_assignee_flow ON public.approval_assignee_configs USING btree (flow_code, enabled);


--
-- Name: idx_approval_assignee_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_approval_assignee_role ON public.approval_assignee_configs USING btree (role_id);


--
-- Name: idx_approval_configs_flow_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_approval_configs_flow_priority ON public.approval_assignee_configs USING btree (flow_code, enabled, priority, sequence_no);


--
-- Name: idx_approval_runtime_nodes_approval; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_approval_runtime_nodes_approval ON public.oa_approval_runtime_nodes USING btree (approval_id, step_no);


--
-- Name: idx_approval_runtime_nodes_due; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_approval_runtime_nodes_due ON public.oa_approval_runtime_nodes USING btree (node_status, due_at);


--
-- Name: idx_approval_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_approval_status ON public.oa_approval_requests USING btree (status, created_at DESC);


--
-- Name: idx_budget_version_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_budget_version_project ON public.biz_project_budget_versions USING btree (tenant_id, project_id, version_no);


--
-- Name: idx_collab_action_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collab_action_source ON public.biz_collaboration_action_logs USING btree (tenant_id, source_type, source_id, created_at);


--
-- Name: idx_collab_task_assignee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collab_task_assignee ON public.biz_collaboration_task_controls USING btree (tenant_id, assignee_user_id, status);


--
-- Name: idx_contract_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contract_customer ON public.crm_service_contracts USING btree (customer_id);


--
-- Name: idx_crm_attachment_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_attachment_entity ON public.crm_attachment USING btree (entity_type, entity_id);


--
-- Name: idx_crm_contact_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_contact_customer ON public.crm_customer_contacts USING btree (customer_id);


--
-- Name: idx_crm_customer_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_customer_name ON public.crm_customers USING btree (tenant_id, name);


--
-- Name: idx_crm_follow_up_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_follow_up_customer ON public.crm_follow_ups USING btree (customer_id, followed_at DESC);


--
-- Name: idx_crm_opportunity_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_opportunity_customer ON public.crm_opportunities USING btree (customer_id);


--
-- Name: idx_crm_quote_opportunity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_quote_opportunity ON public.crm_quote_plans USING btree (opportunity_id);


--
-- Name: idx_crm_site_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_crm_site_customer ON public.crm_customer_sites USING btree (customer_id);


--
-- Name: idx_document_biz; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_document_biz ON public.doc_files USING btree (biz_type, biz_id);


--
-- Name: idx_employee_certificate_expiry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employee_certificate_expiry ON public.hr_employee_certificates USING btree (user_id, expiry_date);


--
-- Name: idx_employee_contract_employee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employee_contract_employee ON public.hr_employee_contracts USING btree (employee_id, end_date);


--
-- Name: idx_entry_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entry_account ON public.fin_accounting_entries USING btree (account_code, voucher_id);


--
-- Name: idx_equipment_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_equipment_customer ON public.maintenance_equipment_assets USING btree (customer_id, status);


--
-- Name: idx_expense_claim_lines_expense; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expense_claim_lines_expense ON public.oa_expense_claim_lines USING btree (expense_id, line_no);


--
-- Name: idx_expense_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expense_project ON public.oa_expense_claims USING btree (project_id, status);


--
-- Name: idx_field_schedule_user_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_field_schedule_user_date ON public.hr_field_schedules USING btree (user_id, work_date);


--
-- Name: idx_goods_receipt_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_goods_receipt_order ON public.procurement_goods_receipts USING btree (order_id, received_date DESC);


--
-- Name: idx_handover_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_handover_status ON public.biz_project_handovers USING btree (tenant_id, status);


--
-- Name: idx_hr_field_attendance_work_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_hr_field_attendance_work_order ON public.hr_field_attendance USING btree (work_order_id);


--
-- Name: idx_inventory_issue_line_issue; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_issue_line_issue ON public.inventory_issue_lines USING btree (issue_id, part_id);


--
-- Name: idx_inventory_issue_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_issue_project ON public.inventory_issue_orders USING btree (project_id, issue_date DESC);


--
-- Name: idx_inventory_movement_part; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_movement_part ON public.inventory_stock_movements USING btree (part_id);


--
-- Name: idx_inventory_return_issue; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_return_issue ON public.inventory_return_orders USING btree (issue_id, return_date DESC);


--
-- Name: idx_inventory_return_line_return; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_return_line_return ON public.inventory_return_lines USING btree (return_id, issue_line_id);


--
-- Name: idx_maintenance_plan_due; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_maintenance_plan_due ON public.maintenance_plans USING btree (active, next_due_date);


--
-- Name: idx_maintenance_plans_asset; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_maintenance_plans_asset ON public.maintenance_plans USING btree (asset_id);


--
-- Name: idx_maintenance_plans_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_maintenance_plans_contract ON public.maintenance_plans USING btree (contract_id);


--
-- Name: idx_notification_read; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_read ON public.system_notifications USING btree (is_read, created_at DESC);


--
-- Name: idx_oa_approval_requests_context; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_oa_approval_requests_context ON public.oa_approval_requests USING btree (approval_type, status, business_type, department_name);


--
-- Name: idx_operation_audit_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_operation_audit_created ON public.operation_audits USING btree (created_at DESC);


--
-- Name: idx_outsource_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outsource_status ON public.oa_outsource_orders USING btree (status, planned_date);


--
-- Name: idx_payment_application_payable; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_application_payable ON public.fin_payment_applications USING btree (payable_id, status);


--
-- Name: idx_payment_application_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_application_status ON public.fin_payment_applications USING btree (status, requested_date DESC);


--
-- Name: idx_payment_record_payable; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_record_payable ON public.fin_payment_records USING btree (payable_id, paid_date DESC);


--
-- Name: idx_proc_action_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_action_source ON public.procurement_action_logs USING btree (tenant_id, source_type, source_id, created_at);


--
-- Name: idx_proc_collab_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_collab_order ON public.procurement_collaboration_events USING btree (tenant_id, order_id, event_type);


--
-- Name: idx_proc_contract_supplier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_contract_supplier ON public.procurement_contracts USING btree (tenant_id, supplier_id, status);


--
-- Name: idx_proc_inquiry_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_inquiry_request ON public.procurement_inquiries USING btree (request_id);


--
-- Name: idx_proc_inquiry_requests_inquiry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_inquiry_requests_inquiry ON public.procurement_inquiry_requests USING btree (tenant_id, inquiry_id);


--
-- Name: idx_proc_inquiry_requests_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_inquiry_requests_request ON public.procurement_inquiry_requests USING btree (tenant_id, request_id);


--
-- Name: idx_proc_invoice_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_invoice_order ON public.procurement_supplier_invoices USING btree (order_id);


--
-- Name: idx_proc_purchase_request_batch; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_purchase_request_batch ON public.procurement_purchase_requests USING btree (tenant_id, batch_id, line_no);


--
-- Name: idx_proc_quote_inquiry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_quote_inquiry ON public.procurement_supplier_quotes USING btree (inquiry_id);


--
-- Name: idx_proc_quote_lines_quote; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_quote_lines_quote ON public.procurement_supplier_quote_lines USING btree (tenant_id, quote_id);


--
-- Name: idx_proc_quote_lines_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_quote_lines_request ON public.procurement_supplier_quote_lines USING btree (tenant_id, request_id);


--
-- Name: idx_proc_supplier_admission; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_supplier_admission ON public.procurement_suppliers USING btree (tenant_id, admission_status, created_at);


--
-- Name: idx_proc_supplier_change; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_supplier_change ON public.procurement_supplier_change_requests USING btree (tenant_id, supplier_id, status);


--
-- Name: idx_proc_supplier_review; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_proc_supplier_review ON public.procurement_supplier_reviews USING btree (tenant_id, supplier_id, review_period);


--
-- Name: idx_procurement_cost_department; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_procurement_cost_department ON public.procurement_cost_allocations USING btree (department_id, incurred_date DESC);


--
-- Name: idx_procurement_cost_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_procurement_cost_order ON public.procurement_cost_allocations USING btree (order_id);


--
-- Name: idx_procurement_cost_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_procurement_cost_project ON public.procurement_cost_allocations USING btree (project_id, incurred_date DESC);


--
-- Name: idx_procurement_payable_supplier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_procurement_payable_supplier ON public.fin_procurement_payables USING btree (supplier_id, due_date);


--
-- Name: idx_project_budget_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_budget_project ON public.project_budget_items USING btree (project_id, category);


--
-- Name: idx_project_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_contract ON public.project_projects USING btree (contract_id);


--
-- Name: idx_project_cost_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_cost_project ON public.project_cost_entries USING btree (project_id, incurred_date DESC);


--
-- Name: idx_project_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_customer ON public.project_projects USING btree (customer_id);


--
-- Name: idx_project_stage_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_stage_project ON public.project_stage_records USING btree (project_id, changed_at DESC);


--
-- Name: idx_purchase_order_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_order_request ON public.procurement_purchase_orders USING btree (request_id);


--
-- Name: idx_purchase_order_supplier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_order_supplier ON public.procurement_purchase_orders USING btree (supplier_id);


--
-- Name: idx_purchase_request_part; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_purchase_request_part ON public.procurement_purchase_requests USING btree (part_id);


--
-- Name: idx_qual_certificate_employee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qual_certificate_employee ON public.qual_personnel_certificates USING btree (employee_id, valid_to);


--
-- Name: idx_qual_certificate_specialty; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qual_certificate_specialty ON public.qual_personnel_certificates USING btree (specialty, valid_to);


--
-- Name: idx_qual_company_expiry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qual_company_expiry ON public.qual_company_qualifications USING btree (valid_to, annual_review_date);


--
-- Name: idx_qual_employee_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qual_employee_name ON public.qual_employees USING btree (name);


--
-- Name: idx_qual_employee_organization; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qual_employee_organization ON public.qual_employees USING btree (organization_id, employment_status);


--
-- Name: idx_qual_performance_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_qual_performance_type ON public.qual_performances USING btree (project_type, subject_company);


--
-- Name: idx_quote_approval_quote; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quote_approval_quote ON public.crm_quote_approval_records USING btree (quote_id, decided_at DESC);


--
-- Name: idx_quote_cost_requests_quote; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quote_cost_requests_quote ON public.crm_quote_cost_requests USING btree (quote_id, created_at DESC);


--
-- Name: idx_quote_cost_requests_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quote_cost_requests_status ON public.crm_quote_cost_requests USING btree (status, requested_at DESC);


--
-- Name: idx_quote_revision_quote; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quote_revision_quote ON public.crm_quote_revisions USING btree (quote_id, version_no DESC);


--
-- Name: idx_receipt_receivable; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_receipt_receivable ON public.fin_receivable_receipts USING btree (receivable_id, received_date DESC);


--
-- Name: idx_receivable_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_receivable_customer ON public.fin_receivables USING btree (customer_id);


--
-- Name: idx_request_approval_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_request_approval_request ON public.procurement_request_approval_records USING btree (request_id, decided_at DESC);


--
-- Name: idx_responsibility_collab_binding; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_responsibility_collab_binding ON public.biz_responsibility_collaborators USING btree (tenant_id, binding_id);


--
-- Name: idx_responsibility_department; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_responsibility_department ON public.biz_responsibility_bindings USING btree (tenant_id, department_id);


--
-- Name: idx_risk_rule_configs_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_risk_rule_configs_module ON public.risk_rule_configs USING btree (module, rule_code);


--
-- Name: idx_risk_snapshots_date_module; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_risk_snapshots_date_module ON public.risk_snapshots USING btree (snapshot_date, module);


--
-- Name: idx_risk_workflow_actions_key_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_risk_workflow_actions_key_created ON public.risk_workflow_actions USING btree (risk_key, created_at DESC);


--
-- Name: idx_risk_workflows_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_risk_workflows_status ON public.risk_workflows USING btree (status, updated_at);


--
-- Name: idx_rp_perm; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_rp_perm ON public.sys_role_permissions USING btree (permission_id);


--
-- Name: idx_rp_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_rp_role ON public.sys_role_permissions USING btree (role_id);


--
-- Name: idx_soft_delete_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_soft_delete_entity ON public.sys_soft_delete_records USING btree (entity_type, entity_id, status);


--
-- Name: idx_soft_delete_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_soft_delete_status ON public.sys_soft_delete_records USING btree (status, requested_at);


--
-- Name: idx_staff_project; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_staff_project ON public.biz_project_staff_assignments USING btree (tenant_id, project_id);


--
-- Name: idx_system_audit_logs_module_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_system_audit_logs_module_created ON public.system_audit_logs USING btree (biz_module, created_at DESC);


--
-- Name: idx_system_audit_logs_operation_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_system_audit_logs_operation_created ON public.system_audit_logs USING btree (operation_type, created_at DESC);


--
-- Name: idx_timesheet_assignment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timesheet_assignment ON public.biz_project_timesheets USING btree (tenant_id, assignment_id, work_date);


--
-- Name: idx_timesheet_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_timesheet_status ON public.biz_project_timesheets USING btree (tenant_id, status, work_date);


--
-- Name: idx_voucher_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_voucher_date ON public.fin_accounting_vouchers USING btree (voucher_date DESC, status);


--
-- Name: idx_work_order_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_work_order_customer ON public.work_orders USING btree (customer_id);


--
-- Name: idx_work_order_equipment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_work_order_equipment ON public.work_orders USING btree (equipment_id, created_at DESC);


--
-- Name: idx_work_order_material; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_work_order_material ON public.work_order_materials USING btree (work_order_id);


--
-- Name: idx_work_order_status_plan; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_work_order_status_plan ON public.work_orders USING btree (status, planned_date);


--
-- Name: uk_contract_quote; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_contract_quote ON public.crm_service_contracts USING btree (quote_id) WHERE (quote_id IS NOT NULL);


--
-- Name: uk_notification_dedup; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_notification_dedup ON public.system_notifications USING btree (tenant_id, dedup_key);


--
-- Name: uk_proc_invoice_client_request; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_proc_invoice_client_request ON public.procurement_supplier_invoices USING btree (tenant_id, client_request_id);


--
-- Name: uk_proc_receipt_client_request; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_proc_receipt_client_request ON public.procurement_goods_receipts USING btree (tenant_id, client_request_id);


--
-- Name: uk_qual_employee_system_user; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_qual_employee_system_user ON public.qual_employees USING btree (system_user_id);


--
-- Name: uk_risk_snapshots_date_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_risk_snapshots_date_key ON public.risk_snapshots USING btree (snapshot_date, risk_key);


--
-- Name: crm_customer_contacts crm_customer_contacts_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_customer_contacts
    ADD CONSTRAINT crm_customer_contacts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.crm_customers(id) ON DELETE CASCADE;


--
-- Name: crm_customer_sites crm_customer_sites_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_customer_sites
    ADD CONSTRAINT crm_customer_sites_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.crm_customers(id) ON DELETE CASCADE;


--
-- Name: crm_follow_ups crm_follow_ups_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_follow_ups
    ADD CONSTRAINT crm_follow_ups_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.crm_customers(id);


--
-- Name: crm_follow_ups crm_follow_ups_opportunity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_follow_ups
    ADD CONSTRAINT crm_follow_ups_opportunity_id_fkey FOREIGN KEY (opportunity_id) REFERENCES public.crm_opportunities(id);


--
-- Name: crm_opportunities crm_opportunities_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_opportunities
    ADD CONSTRAINT crm_opportunities_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.crm_customers(id);


--
-- Name: crm_quote_approval_records crm_quote_approval_records_generated_contract_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_approval_records
    ADD CONSTRAINT crm_quote_approval_records_generated_contract_id_fkey FOREIGN KEY (generated_contract_id) REFERENCES public.crm_service_contracts(id);


--
-- Name: crm_quote_approval_records crm_quote_approval_records_quote_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_approval_records
    ADD CONSTRAINT crm_quote_approval_records_quote_id_fkey FOREIGN KEY (quote_id) REFERENCES public.crm_quote_plans(id);


--
-- Name: crm_quote_plans crm_quote_plans_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_plans
    ADD CONSTRAINT crm_quote_plans_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.crm_customers(id);


--
-- Name: crm_quote_plans crm_quote_plans_opportunity_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_plans
    ADD CONSTRAINT crm_quote_plans_opportunity_id_fkey FOREIGN KEY (opportunity_id) REFERENCES public.crm_opportunities(id);


--
-- Name: crm_quote_revisions crm_quote_revisions_quote_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_quote_revisions
    ADD CONSTRAINT crm_quote_revisions_quote_id_fkey FOREIGN KEY (quote_id) REFERENCES public.crm_quote_plans(id);


--
-- Name: crm_service_contracts crm_service_contracts_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_service_contracts
    ADD CONSTRAINT crm_service_contracts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.crm_customers(id);


--
-- Name: crm_service_contracts crm_service_contracts_quote_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crm_service_contracts
    ADD CONSTRAINT crm_service_contracts_quote_id_fkey FOREIGN KEY (quote_id) REFERENCES public.crm_quote_plans(id);


--
-- Name: fin_accounting_entries fin_accounting_entries_voucher_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_accounting_entries
    ADD CONSTRAINT fin_accounting_entries_voucher_id_fkey FOREIGN KEY (voucher_id) REFERENCES public.fin_accounting_vouchers(id);


--
-- Name: fin_payment_applications fin_payment_applications_payable_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_applications
    ADD CONSTRAINT fin_payment_applications_payable_id_fkey FOREIGN KEY (payable_id) REFERENCES public.fin_procurement_payables(id);


--
-- Name: fin_payment_applications fin_payment_applications_supplier_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_applications
    ADD CONSTRAINT fin_payment_applications_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.procurement_suppliers(id);


--
-- Name: fin_payment_records fin_payment_records_application_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_records
    ADD CONSTRAINT fin_payment_records_application_id_fkey FOREIGN KEY (application_id) REFERENCES public.fin_payment_applications(id);


--
-- Name: fin_payment_records fin_payment_records_payable_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_records
    ADD CONSTRAINT fin_payment_records_payable_id_fkey FOREIGN KEY (payable_id) REFERENCES public.fin_procurement_payables(id);


--
-- Name: fin_payment_records fin_payment_records_supplier_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_payment_records
    ADD CONSTRAINT fin_payment_records_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.procurement_suppliers(id);


--
-- Name: fin_procurement_payables fin_procurement_payables_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_procurement_payables
    ADD CONSTRAINT fin_procurement_payables_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.procurement_purchase_orders(id);


--
-- Name: fin_procurement_payables fin_procurement_payables_receipt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_procurement_payables
    ADD CONSTRAINT fin_procurement_payables_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.procurement_goods_receipts(id);


--
-- Name: fin_procurement_payables fin_procurement_payables_supplier_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_procurement_payables
    ADD CONSTRAINT fin_procurement_payables_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.procurement_suppliers(id);


--
-- Name: fin_receivable_receipts fin_receivable_receipts_receivable_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_receivable_receipts
    ADD CONSTRAINT fin_receivable_receipts_receivable_id_fkey FOREIGN KEY (receivable_id) REFERENCES public.fin_receivables(id);


--
-- Name: fin_receivables fin_receivables_contract_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_receivables
    ADD CONSTRAINT fin_receivables_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES public.crm_service_contracts(id);


--
-- Name: fin_receivables fin_receivables_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fin_receivables
    ADD CONSTRAINT fin_receivables_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.crm_customers(id);


--
-- Name: sys_organizations fk3a9hevn6cq5di6a2kpix3l2j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_organizations
    ADD CONSTRAINT fk3a9hevn6cq5di6a2kpix3l2j FOREIGN KEY (parent_id) REFERENCES public.sys_organizations(id);


--
-- Name: hr_leave_requests fk61ujnvrtixlwupo78ednrsr0e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_leave_requests
    ADD CONSTRAINT fk61ujnvrtixlwupo78ednrsr0e FOREIGN KEY (employee_id) REFERENCES public.qual_employees(id);


--
-- Name: hr_employee_work_experience fk6rseatfgvlrp4cr20wughh5id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_work_experience
    ADD CONSTRAINT fk6rseatfgvlrp4cr20wughh5id FOREIGN KEY (employee_id) REFERENCES public.qual_employees(id);


--
-- Name: approval_assignee_configs fk_approval_assignee_escalation_role; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.approval_assignee_configs
    ADD CONSTRAINT fk_approval_assignee_escalation_role FOREIGN KEY (escalation_role_id) REFERENCES public.sys_roles(id);


--
-- Name: approval_assignee_configs fk_approval_assignee_role; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.approval_assignee_configs
    ADD CONSTRAINT fk_approval_assignee_role FOREIGN KEY (role_id) REFERENCES public.sys_roles(id);


--
-- Name: qual_employees fk_qual_employee_organization; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_employees
    ADD CONSTRAINT fk_qual_employee_organization FOREIGN KEY (organization_id) REFERENCES public.sys_organizations(id);


--
-- Name: hr_employee_lifecycle_records fkawmipfr8wihgy061pb4b5sno9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_lifecycle_records
    ADD CONSTRAINT fkawmipfr8wihgy061pb4b5sno9 FOREIGN KEY (employee_id) REFERENCES public.qual_employees(id);


--
-- Name: hr_employee_education fkko3opqs98ddh9v12bnxqeh077; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_education
    ADD CONSTRAINT fkko3opqs98ddh9v12bnxqeh077 FOREIGN KEY (employee_id) REFERENCES public.qual_employees(id);


--
-- Name: hr_leave_balances fkq400lujsvd7n5py40r8lovtr3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_leave_balances
    ADD CONSTRAINT fkq400lujsvd7n5py40r8lovtr3 FOREIGN KEY (employee_id) REFERENCES public.qual_employees(id);


--
-- Name: hr_emergency_contacts hr_emergency_contacts_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_emergency_contacts
    ADD CONSTRAINT hr_emergency_contacts_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.qual_employees(id);


--
-- Name: hr_employee_certificates hr_employee_certificates_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_certificates
    ADD CONSTRAINT hr_employee_certificates_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.sys_users(id);


--
-- Name: hr_employee_contracts hr_employee_contracts_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_employee_contracts
    ADD CONSTRAINT hr_employee_contracts_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.qual_employees(id) ON DELETE CASCADE;


--
-- Name: hr_field_attendance hr_field_attendance_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_field_attendance
    ADD CONSTRAINT hr_field_attendance_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.sys_users(id);


--
-- Name: hr_field_attendance hr_field_attendance_work_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_field_attendance
    ADD CONSTRAINT hr_field_attendance_work_order_id_fkey FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);


--
-- Name: hr_field_schedules hr_field_schedules_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.hr_field_schedules
    ADD CONSTRAINT hr_field_schedules_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.sys_users(id);


--
-- Name: inventory_issue_lines inventory_issue_lines_issue_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_issue_lines
    ADD CONSTRAINT inventory_issue_lines_issue_id_fkey FOREIGN KEY (issue_id) REFERENCES public.inventory_issue_orders(id);


--
-- Name: inventory_issue_lines inventory_issue_lines_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_issue_lines
    ADD CONSTRAINT inventory_issue_lines_part_id_fkey FOREIGN KEY (part_id) REFERENCES public.inventory_parts(id);


--
-- Name: inventory_issue_orders inventory_issue_orders_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_issue_orders
    ADD CONSTRAINT inventory_issue_orders_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- Name: inventory_return_lines inventory_return_lines_issue_line_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_return_lines
    ADD CONSTRAINT inventory_return_lines_issue_line_id_fkey FOREIGN KEY (issue_line_id) REFERENCES public.inventory_issue_lines(id);


--
-- Name: inventory_return_lines inventory_return_lines_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_return_lines
    ADD CONSTRAINT inventory_return_lines_part_id_fkey FOREIGN KEY (part_id) REFERENCES public.inventory_parts(id);


--
-- Name: inventory_return_lines inventory_return_lines_return_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_return_lines
    ADD CONSTRAINT inventory_return_lines_return_id_fkey FOREIGN KEY (return_id) REFERENCES public.inventory_return_orders(id);


--
-- Name: inventory_return_orders inventory_return_orders_issue_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_return_orders
    ADD CONSTRAINT inventory_return_orders_issue_id_fkey FOREIGN KEY (issue_id) REFERENCES public.inventory_issue_orders(id);


--
-- Name: inventory_return_orders inventory_return_orders_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_return_orders
    ADD CONSTRAINT inventory_return_orders_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- Name: inventory_stock_movements inventory_stock_movements_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_stock_movements
    ADD CONSTRAINT inventory_stock_movements_part_id_fkey FOREIGN KEY (part_id) REFERENCES public.inventory_parts(id);


--
-- Name: maintenance_equipment_assets maintenance_equipment_assets_contract_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_equipment_assets
    ADD CONSTRAINT maintenance_equipment_assets_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES public.crm_service_contracts(id);


--
-- Name: maintenance_equipment_assets maintenance_equipment_assets_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_equipment_assets
    ADD CONSTRAINT maintenance_equipment_assets_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.crm_customers(id);


--
-- Name: maintenance_plans maintenance_plans_asset_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_plans
    ADD CONSTRAINT maintenance_plans_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.maintenance_equipment_assets(id);


--
-- Name: maintenance_plans maintenance_plans_contract_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maintenance_plans
    ADD CONSTRAINT maintenance_plans_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES public.crm_service_contracts(id);


--
-- Name: oa_approval_actions oa_approval_actions_approval_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_approval_actions
    ADD CONSTRAINT oa_approval_actions_approval_id_fkey FOREIGN KEY (approval_id) REFERENCES public.oa_approval_requests(id);


--
-- Name: oa_expense_claim_lines oa_expense_claim_lines_expense_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_expense_claim_lines
    ADD CONSTRAINT oa_expense_claim_lines_expense_id_fkey FOREIGN KEY (expense_id) REFERENCES public.oa_expense_claims(id) ON DELETE CASCADE;


--
-- Name: oa_expense_claims oa_expense_claims_approval_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_expense_claims
    ADD CONSTRAINT oa_expense_claims_approval_request_id_fkey FOREIGN KEY (approval_request_id) REFERENCES public.oa_approval_requests(id);


--
-- Name: oa_expense_claims oa_expense_claims_claimant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_expense_claims
    ADD CONSTRAINT oa_expense_claims_claimant_id_fkey FOREIGN KEY (claimant_id) REFERENCES public.sys_users(id);


--
-- Name: oa_expense_claims oa_expense_claims_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_expense_claims
    ADD CONSTRAINT oa_expense_claims_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- Name: oa_expense_claims oa_expense_claims_work_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_expense_claims
    ADD CONSTRAINT oa_expense_claims_work_order_id_fkey FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);


--
-- Name: oa_outsource_orders oa_outsource_orders_approval_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_outsource_orders
    ADD CONSTRAINT oa_outsource_orders_approval_request_id_fkey FOREIGN KEY (approval_request_id) REFERENCES public.oa_approval_requests(id);


--
-- Name: oa_outsource_orders oa_outsource_orders_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_outsource_orders
    ADD CONSTRAINT oa_outsource_orders_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- Name: oa_outsource_orders oa_outsource_orders_supplier_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_outsource_orders
    ADD CONSTRAINT oa_outsource_orders_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.procurement_suppliers(id);


--
-- Name: oa_outsource_orders oa_outsource_orders_work_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oa_outsource_orders
    ADD CONSTRAINT oa_outsource_orders_work_order_id_fkey FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);


--
-- Name: procurement_cost_allocations procurement_cost_allocations_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_cost_allocations
    ADD CONSTRAINT procurement_cost_allocations_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.sys_organizations(id);


--
-- Name: procurement_cost_allocations procurement_cost_allocations_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_cost_allocations
    ADD CONSTRAINT procurement_cost_allocations_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.procurement_purchase_orders(id);


--
-- Name: procurement_cost_allocations procurement_cost_allocations_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_cost_allocations
    ADD CONSTRAINT procurement_cost_allocations_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- Name: procurement_cost_allocations procurement_cost_allocations_receipt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_cost_allocations
    ADD CONSTRAINT procurement_cost_allocations_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.procurement_goods_receipts(id);


--
-- Name: procurement_goods_receipts procurement_goods_receipts_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_goods_receipts
    ADD CONSTRAINT procurement_goods_receipts_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.procurement_purchase_orders(id);


--
-- Name: procurement_goods_receipts procurement_goods_receipts_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_goods_receipts
    ADD CONSTRAINT procurement_goods_receipts_part_id_fkey FOREIGN KEY (part_id) REFERENCES public.inventory_parts(id);


--
-- Name: procurement_purchase_orders procurement_purchase_orders_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_orders
    ADD CONSTRAINT procurement_purchase_orders_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.sys_organizations(id);


--
-- Name: procurement_purchase_orders procurement_purchase_orders_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_orders
    ADD CONSTRAINT procurement_purchase_orders_part_id_fkey FOREIGN KEY (part_id) REFERENCES public.inventory_parts(id);


--
-- Name: procurement_purchase_orders procurement_purchase_orders_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_orders
    ADD CONSTRAINT procurement_purchase_orders_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- Name: procurement_purchase_orders procurement_purchase_orders_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_orders
    ADD CONSTRAINT procurement_purchase_orders_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.procurement_purchase_requests(id);


--
-- Name: procurement_purchase_orders procurement_purchase_orders_supplier_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_orders
    ADD CONSTRAINT procurement_purchase_orders_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.procurement_suppliers(id);


--
-- Name: procurement_purchase_requests procurement_purchase_requests_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_requests
    ADD CONSTRAINT procurement_purchase_requests_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.sys_organizations(id);


--
-- Name: procurement_purchase_requests procurement_purchase_requests_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_requests
    ADD CONSTRAINT procurement_purchase_requests_part_id_fkey FOREIGN KEY (part_id) REFERENCES public.inventory_parts(id);


--
-- Name: procurement_purchase_requests procurement_purchase_requests_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_purchase_requests
    ADD CONSTRAINT procurement_purchase_requests_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- Name: procurement_request_approval_records procurement_request_approval_records_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.procurement_request_approval_records
    ADD CONSTRAINT procurement_request_approval_records_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.procurement_purchase_requests(id);


--
-- Name: project_budget_items project_budget_items_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_budget_items
    ADD CONSTRAINT project_budget_items_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- Name: project_cost_entries project_cost_entries_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_cost_entries
    ADD CONSTRAINT project_cost_entries_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- Name: project_projects project_projects_contract_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_projects
    ADD CONSTRAINT project_projects_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES public.crm_service_contracts(id);


--
-- Name: project_projects project_projects_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_projects
    ADD CONSTRAINT project_projects_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.crm_customers(id);


--
-- Name: project_stage_records project_stage_records_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_stage_records
    ADD CONSTRAINT project_stage_records_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- Name: qual_employees qual_employees_system_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_employees
    ADD CONSTRAINT qual_employees_system_user_id_fkey FOREIGN KEY (system_user_id) REFERENCES public.sys_users(id) ON DELETE SET NULL;


--
-- Name: qual_personnel_certificates qual_personnel_certificates_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.qual_personnel_certificates
    ADD CONSTRAINT qual_personnel_certificates_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.qual_employees(id) ON DELETE CASCADE;


--
-- Name: sys_role_data_organizations sys_role_data_organizations_organization_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_data_organizations
    ADD CONSTRAINT sys_role_data_organizations_organization_id_fkey FOREIGN KEY (organization_id) REFERENCES public.sys_organizations(id);


--
-- Name: sys_role_data_organizations sys_role_data_organizations_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_data_organizations
    ADD CONSTRAINT sys_role_data_organizations_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.sys_roles(id) ON DELETE CASCADE;


--
-- Name: sys_role_permissions sys_role_permissions_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_permissions
    ADD CONSTRAINT sys_role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.sys_permissions(id) ON DELETE CASCADE;


--
-- Name: sys_role_permissions sys_role_permissions_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role_permissions
    ADD CONSTRAINT sys_role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.sys_roles(id) ON DELETE CASCADE;


--
-- Name: sys_user_roles sys_user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_roles
    ADD CONSTRAINT sys_user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.sys_roles(id) ON DELETE CASCADE;


--
-- Name: sys_user_roles sys_user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_roles
    ADD CONSTRAINT sys_user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.sys_users(id) ON DELETE CASCADE;


--
-- Name: sys_users sys_users_org_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_users
    ADD CONSTRAINT sys_users_org_id_fkey FOREIGN KEY (org_id) REFERENCES public.sys_organizations(id);


--
-- Name: system_notifications system_notifications_target_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_notifications
    ADD CONSTRAINT system_notifications_target_user_id_fkey FOREIGN KEY (target_user_id) REFERENCES public.sys_users(id);


--
-- Name: work_order_materials work_order_materials_part_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_materials
    ADD CONSTRAINT work_order_materials_part_id_fkey FOREIGN KEY (part_id) REFERENCES public.inventory_parts(id);


--
-- Name: work_order_materials work_order_materials_work_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_materials
    ADD CONSTRAINT work_order_materials_work_order_id_fkey FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);


--
-- Name: work_order_status_logs work_order_status_logs_work_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_order_status_logs
    ADD CONSTRAINT work_order_status_logs_work_order_id_fkey FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);


--
-- Name: work_orders work_orders_assignee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_assignee_id_fkey FOREIGN KEY (assignee_id) REFERENCES public.sys_users(id);


--
-- Name: work_orders work_orders_contract_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES public.crm_service_contracts(id);


--
-- Name: work_orders work_orders_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.crm_customers(id);


--
-- Name: work_orders work_orders_equipment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_equipment_id_fkey FOREIGN KEY (equipment_id) REFERENCES public.maintenance_equipment_assets(id);


--
-- Name: work_orders work_orders_maintenance_plan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_maintenance_plan_id_fkey FOREIGN KEY (maintenance_plan_id) REFERENCES public.maintenance_plans(id);


--
-- Name: work_orders work_orders_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project_projects(id);


--
-- PostgreSQL database dump complete
--
