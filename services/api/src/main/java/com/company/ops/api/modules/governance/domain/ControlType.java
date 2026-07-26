package com.company.ops.api.modules.governance.domain;

public enum ControlType {
  CREDIT_CONTROL("FINANCE", "客户信用与坏账"),
  INVOICE_TAX("FINANCE", "发票红冲与税务状态"),
  EXPENSE_PAYMENT("FINANCE", "费用到付款闭环"),
  CASH_FORECAST("FINANCE", "资金滚动预测"),
  CONTRACT_MILESTONE("CRM", "合同里程碑与交付验收"),
  REVENUE_OBLIGATION("CRM", "履约义务与收入确认"),
  CONTRACT_CHANGE("CRM", "合同变更、签证与索赔"),
  WARRANTY_RENEWAL("CRM", "质保与续约"),
  PROJECT_WBS("PROJECT", "项目WBS与基线"),
  PROJECT_FORECAST("PROJECT", "项目EAC/ETC与现金流预测"),
  PROJECT_CLOSEOUT("PROJECT", "项目结项与质保移交"),
  RESOURCE_CAPACITY("PROJECT", "资源负荷与成本率"),
  PURCHASE_BUDGET("PROCUREMENT", "采购预算占用与释放"),
  PRICE_CONTROL("PROCUREMENT", "协议价与价格偏离"),
  MATCH_TOLERANCE("PROCUREMENT", "三单匹配容差"),
  SUPPLIER_GOVERNANCE("PROCUREMENT", "供应商准入、绩效与黑名单"),
  PURCHASE_CLAIM("PROCUREMENT", "采购退货、索赔与委外"),
  WAREHOUSE_LOCATION("INVENTORY", "仓库与库位"),
  LOT_SERIAL("INVENTORY", "批次与序列号"),
  STOCK_COUNT("INVENTORY", "盘点冻结与差异审批"),
  INVENTORY_POLICY("INVENTORY", "安全库存、预留与呆滞料"),
  FIXED_ASSET("ASSET", "固定资产全生命周期"),
  SERVICE_SLA("SERVICE", "服务SLA与暂停时钟"),
  SERVICE_ENTITLEMENT("SERVICE", "服务合同权益与计费"),
  SERVICE_QUALITY("SERVICE", "一次修复率与客户满意度"),
  MAINTENANCE_STRATEGY("SERVICE", "预防维护与设备健康"),
  MASTER_DATA_CHANGE("MASTER_DATA", "主数据变更与生效审批"),
  MASTER_DATA_DEDUP("MASTER_DATA", "主数据查重与合并"),
  LEGAL_ENTITY("MASTER_DATA", "法人、账套与内部交易"),
  KPI_DEFINITION("BI", "KPI口径、版本与钻取"),
  BUSINESS_FORECAST("BI", "商机、回款、采购与补货预测");

  private final String domain;
  private final String label;

  ControlType(String domain, String label) {
    this.domain = domain;
    this.label = label;
  }

  public String domain() { return domain; }
  public String label() { return label; }
}
