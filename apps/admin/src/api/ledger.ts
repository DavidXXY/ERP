import { request } from "./http";
export type VoucherEntry={id:string;accountCode:string;accountName:string;debit:number;credit:number;summary?:string};
export type AccountingVoucher={id:string;code:string;bizType:string;bizNo:string;voucherDate:string;description:string;status:"POSTED"|"REVERSED";totalDebit:number;totalCredit:number;entries:VoucherEntry[]};
export type LedgerOverview={voucherCount:number;totalDebit:number;totalCredit:number;revenue:number;expense:number;profit:number;cashBalance:number};
export type StatementLine={accountCode:string;accountName:string;debit:number;credit:number;balance:number};
export type FinancialStatements={assets:StatementLine[];liabilities:StatementLine[];revenue:StatementLine[];expenses:StatementLine[];totalAssets:number;totalLiabilities:number;totalRevenue:number;totalExpense:number;profit:number;netCashFlow:number};
export function getLedgerOverview(){return request<LedgerOverview>({method:"GET",url:"/finance/ledger/overview"});}
export function listVouchers(){return request<AccountingVoucher[]>({method:"GET",url:"/finance/ledger/vouchers"});}
export function getFinancialStatements(){return request<FinancialStatements>({method:"GET",url:"/finance/ledger/statements"});}
