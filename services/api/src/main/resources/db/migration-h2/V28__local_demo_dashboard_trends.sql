-- Populate six months of balanced revenue, expense and cash-flow demo vouchers.

insert into fin_accounting_vouchers (
  code, biz_type, biz_no, voucher_date, description, status, total_debit, total_credit
)
select 'DEMO-TREND-REV-' || r."X", 'DEMO_REVENUE', 'DEMO-TREND-REV-' || r."X",
       dateadd('MONTH', -r."X", current_date), '演示月度服务收入', 'POSTED',
       180000 + r."X" * 20000, 180000 + r."X" * 20000
from system_range(0, 5) r;

insert into fin_accounting_entries (voucher_id, account_code, account_name, debit, credit, summary)
select voucher.id, '1002', '银行存款', voucher.total_debit, 0, '演示服务回款'
from fin_accounting_vouchers voucher
where voucher.code like 'DEMO-TREND-REV-%';

insert into fin_accounting_entries (voucher_id, account_code, account_name, debit, credit, summary)
select voucher.id, '6001', '主营业务收入', 0, voucher.total_credit, '演示服务收入'
from fin_accounting_vouchers voucher
where voucher.code like 'DEMO-TREND-REV-%';

insert into fin_accounting_vouchers (
  code, biz_type, biz_no, voucher_date, description, status, total_debit, total_credit
)
select 'DEMO-TREND-EXP-' || r."X", 'DEMO_EXPENSE', 'DEMO-TREND-EXP-' || r."X",
       dateadd('MONTH', -r."X", current_date), '演示月度经营成本', 'POSTED',
       90000 + r."X" * 8000, 90000 + r."X" * 8000
from system_range(0, 5) r;

insert into fin_accounting_entries (voucher_id, account_code, account_name, debit, credit, summary)
select voucher.id, '6401', '主营业务成本', voucher.total_debit, 0, '演示经营成本'
from fin_accounting_vouchers voucher
where voucher.code like 'DEMO-TREND-EXP-%';

insert into fin_accounting_entries (voucher_id, account_code, account_name, debit, credit, summary)
select voucher.id, '1002', '银行存款', 0, voucher.total_credit, '演示成本支出'
from fin_accounting_vouchers voucher
where voucher.code like 'DEMO-TREND-EXP-%';
