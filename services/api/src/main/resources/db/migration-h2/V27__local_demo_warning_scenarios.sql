-- Put demo records inside the filtered renewal and qualification-warning windows.

update crm_service_contracts
set end_date = dateadd('DAY', 60, current_date),
    status = case when mod(cast(right(code, 3) as integer), 4) = 0 then 'RENEWAL_PENDING' else 'ACTIVE' end,
    updated_at = current_timestamp
where code like 'DEMO-CONTRACT-%';

update qual_company_qualifications
set annual_review_date = dateadd('DAY', 45, current_date),
    renewal_date = dateadd('DAY', 90, current_date),
    updated_at = current_timestamp
where external_id like 'DEMO-CQ-%';

update qual_personnel_certificates
set review_date = dateadd('DAY', 60, current_date),
    updated_at = current_timestamp
where external_id like 'DEMO-PC-%';
