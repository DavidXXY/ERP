import { request, requestAllPages } from "./http";
export type VoucherEntry = {
  id: string;
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
  summary?: string;
};
export type AccountingVoucher = {
  id: string;
  code: string;
  bizType: string;
  bizNo: string;
  voucherDate: string;
  description: string;
  status: "DRAFT" | "REVIEWED" | "POSTED" | "REVERSED";
  totalDebit: number;
  totalCredit: number;
  entries: VoucherEntry[];
  reviewedAt?: string;
  reviewedBy?: string;
  postedAt?: string;
  postedBy?: string;
  reversedAt?: string;
  reversedBy?: string;
  reversalReason?: string;
  reversalVoucherId?: string;
};
export type LedgerOverview = {
  voucherCount: number;
  totalDebit: number;
  totalCredit: number;
  revenue: number;
  expense: number;
  profit: number;
  cashBalance: number;
};
export type StatementLine = {
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
  balance: number;
};
export type FinancialStatements = {
  assets: StatementLine[];
  liabilities: StatementLine[];
  revenue: StatementLine[];
  expenses: StatementLine[];
  totalAssets: number;
  totalLiabilities: number;
  totalRevenue: number;
  totalExpense: number;
  profit: number;
  netCashFlow: number;
};
export function getLedgerOverview() {
  return request<LedgerOverview>({
    method: "GET",
    url: "/finance/ledger/overview",
  });
}
export function listVouchers() {
  return requestAllPages<AccountingVoucher>(
    {
      method: "GET",
      url: "/finance/ledger/vouchers",
    },
    200,
  );
}
export function getFinancialStatements() {
  return request<FinancialStatements>({
    method: "GET",
    url: "/finance/ledger/statements",
  });
}
export type CreateVoucherPayload = {
  bizType: string;
  bizNo: string;
  voucherDate: string;
  description: string;
  lines: {
    accountCode: string;
    accountName: string;
    debit?: number;
    credit?: number;
    summary?: string;
  }[];
};
export const createVoucherDraft = (data: CreateVoucherPayload) =>
  request<AccountingVoucher>({
    method: "POST",
    url: "/finance/ledger/vouchers",
    data,
  });
export const reviewVoucher = (id: string) =>
  request<AccountingVoucher>({
    method: "POST",
    url: `/finance/ledger/vouchers/${id}/review`,
  });
export const postVoucher = (id: string) =>
  request<AccountingVoucher>({
    method: "POST",
    url: `/finance/ledger/vouchers/${id}/post`,
  });
export const reverseVoucher = (
  id: string,
  reversalDate: string,
  reason: string,
) =>
  request<AccountingVoucher>({
    method: "POST",
    url: `/finance/ledger/vouchers/${id}/reverse`,
    data: { reversalDate, reason },
  });
