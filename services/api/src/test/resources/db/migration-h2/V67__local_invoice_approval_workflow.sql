alter table fin_receivables add column if not exists invoice_request_status varchar(32) not null default 'NOT_REQUESTED';
alter table fin_receivables add column if not exists invoice_reviewed_by varchar(80);
alter table fin_receivables add column if not exists invoice_reviewed_at timestamp with time zone;
alter table fin_receivables add column if not exists invoice_review_comment varchar(500);

update fin_receivables
set invoice_request_status = case
  when invoice_no is not null and trim(invoice_no) <> '' then 'INVOICED'
  when invoice_requested = true then 'PENDING_APPROVAL'
  else 'NOT_REQUESTED'
end;
