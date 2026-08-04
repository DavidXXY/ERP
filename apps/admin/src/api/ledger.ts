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
  from: string;
  to: string;
  assets: StatementLine[];
  liabilities: StatementLine[];
  equity: StatementLine[];
  revenue: StatementLine[];
  expenses: StatementLine[];
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
  totalLiabilitiesAndEquity: number;
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
export function getFinancialStatements(params?: {
  from?: string;
  to?: string;
}) {
  return request<FinancialStatements>({
    method: "GET",
    url: "/finance/ledger/statements",
    params,
  });
}
export type AccountingAccount = {
  id: string;
  code: string;
  name: string;
  category: "ASSET" | "LIABILITY" | "EQUITY" | "REVENUE" | "EXPENSE";
  normalDirection: "DEBIT" | "CREDIT";
  cashAccount: boolean;
  active: boolean;
  systemAccount: boolean;
};
export type SaveAccountPayload = Omit<
  AccountingAccount,
  "id" | "systemAccount"
>;
export type OpeningBalance = {
  id: string;
  fiscalYear: number;
  accountCode: string;
  accountName: string;
  debitBalance: number;
  creditBalance: number;
  note?: string;
};
export function listAccountingAccounts() {
  return request<AccountingAccount[]>({
    method: "GET",
    url: "/finance/ledger/accounts",
  });
}
export function createAccountingAccount(data: SaveAccountPayload) {
  return request<AccountingAccount>({
    method: "POST",
    url: "/finance/ledger/accounts",
    data,
  });
}
export function updateAccountingAccount(id: string, data: SaveAccountPayload) {
  return request<AccountingAccount>({
    method: "PUT",
    url: `/finance/ledger/accounts/${id}`,
    data,
  });
}
export function listOpeningBalances(fiscalYear: number) {
  return request<OpeningBalance[]>({
    method: "GET",
    url: "/finance/ledger/opening-balances",
    params: { fiscalYear },
  });
}
export function saveOpeningBalance(data: {
  fiscalYear: number;
  accountCode: string;
  debitBalance: number;
  creditBalance: number;
  note?: string;
}) {
  return request<OpeningBalance>({
    method: "POST",
    url: "/finance/ledger/opening-balances",
    data,
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
