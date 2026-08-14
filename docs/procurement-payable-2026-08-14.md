# 采购应付逻辑说明（2026-08-14，迁移版本 V135）

## 一、应付的形成

- 到货质检合格数量大于零时，按 `合格数量 × 订单单价`（含税口径）生成 `fin_procurement_payables`，金额与税率快照自订单，状态为 `PENDING`。
- 应付到期日：质检时若未指定，按供应商账期 `procurement_suppliers.payment_terms_days`（默认 30 天）自到货日期自动计算。
- 不合格数量自动生成 `OPEN` 退换货记录，走换货/折让/索赔结案。

## 二、应付的口径与冲减

- 有效应付 = `amount - adjusted_amount`；待付 = 有效应付 - 已付；多付部分为待退金额。
- 冲减来源两类，均写入 `fin_procurement_payable_adjustments`：
  - 退换货结案折让/索赔：按到期日顺序逐笔冲减同一订单的未结应付，自动过账。
  - 财务手动冲减：`POST /api/finance/payables/{id}/adjustments`（折让/索赔/更正），不能超过待付金额。
- 应付作废：`POST /api/finance/payables/{id}/cancel`，仅限未付款、无未结付款申请、无已登记发票的应付单；作废自动补齐冲减记录并记录作废人/原因/时间。
- 索赔冲减过账 `6111 其他业务收入`，其余冲减过 `1405 库存商品`（借方均为 `2202 应付账款`）。

## 三、发票与匹配

- 发票登记 `POST /api/procurement/supplier-invoices` 支持 `payableIds` 一单多应付合并开票，同一订单的多笔应付可合并到一张发票。
- 匹配容差：金额差绝对值不超过 `max(发票金额×0.5%, 0.01 元)` 视为匹配，容差内异常可填审核意见通过。
- 发票审核通过后过账：`1405 库存商品`（净额）、`22210101 应交增值税-进项税额`（税额）、`2202 应付账款`（贷，价税合计）。
- 验真：`POST /api/procurement/supplier-invoices/{id}/verify` 记录验真结果、验真人与时间；匹配发票登记时自动标记已验真。

## 四、付款（含按付款方式拆分）

- 两条付款入口统一落地到 `fin_payment_records`，`source_type` 区分 `APPLICATION`（付款申请）与 `DIRECT`（采购直付），并保留 `application_id` 可空。
- 一个订单/应付单可按付款方式拆分多次支付：请求体 `payments[]` 每笔包含应付单、金额、付款日期、付款方式、银行流水/凭证号、备注，逐笔生成付款流水并过账 `2202 应付账款` / `1002 银行存款`。
- 合并付款申请：`POST /api/finance/payment-applications` 支持 `payableIds` 选择同一供应商的多笔应付，按可申请额度比例分配占用；执行付款时每笔拆分可指定对应应付单。
- 并发与防超付：付款执行与直付均使用 `findByIdForUpdate` 悲观锁，逐笔校验不超过应付余额，流水号唯一防重。
- 职责分离：付款申请（`finance:payment:apply`）、审批（`finance:payment:approve`）、执行（`finance:payment:execute`）必须由不同人员完成；采购直付入口为 `procurement:payable:view`。

## 五、关键科目

| 科目 | 用途 |
| --- | --- |
| `2202 应付账款` | 付款/冲减借方 |
| `1002 银行存款` | 付款贷方 |
| `1405 库存商品` | 发票入账与普通冲减 |
| `22210101 应交增值税-进项税额` | 发票税额 |
| `6111 其他业务收入` | 供应商索赔冲减 |
