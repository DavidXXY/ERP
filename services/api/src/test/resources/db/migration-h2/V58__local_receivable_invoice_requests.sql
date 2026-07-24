alter table fin_receivables add column if not exists invoice_requested boolean default false;
alter table fin_receivables add column if not exists invoice_requested_by varchar(80);
alter table fin_receivables add column if not exists invoice_requested_at timestamp with time zone;
alter table fin_receivables add column if not exists invoice_request_remark varchar(500);
update fin_receivables set invoice_requested=false where invoice_requested is null;
alter table fin_receivables alter column invoice_requested set not null;
