import { describe, expect, it } from "vitest";

const viewSources = {
  ...import.meta.glob("../views/**/*.vue", {
    eager: true,
    query: "?raw",
    import: "default",
  }),
  ...import.meta.glob("../components/**/*.vue", {
    eager: true,
    query: "?raw",
    import: "default",
  }),
} as Record<string, string>;

const explicitMoneyLabel =
  /(?:金额|单价|总价|合同额|合同总额|报价总额|预算成本|预算余额|成本总额|材料成本|外包成本|实际成本|人工成本|单位成本|库存金额|库存总价值|库存资产|营业收入|期间收入|合同收入|累计回款|待收款项|应收总额|应付总额|待开票|待回款|逾期应收|逾期应付|已回款|已付款|待付款|已付金额|待付金额|已收金额|待收金额|毛利|利润|人时产值|资金净流量|现金净流|现金流|净流量|金额敞口|借方|贷方|余额)$/;
const taxBasis = /含税|未税|不含税|税价不适用|税价随来源单据/;
const moneyUnit = /元|万元/;
const nonMoneyLabels = new Set([
  "CRM逾期应收",
  "存在逾期应收",
  "逾期应收",
  "待开票",
  "待回款",
  "待付款",
  "已付款",
  "项目利润",
  "利润",
]);
const labelPattern = /(?:label|title)="([^"]+)"|(?:label|title):\s*"([^"]+)"/g;

describe("money labels", () => {
  it("states tax basis and unit for explicit monetary fields", () => {
    const ambiguous: string[] = [];

    Object.entries(viewSources).forEach(([file, source]) => {
      for (const match of source.matchAll(labelPattern)) {
        const label = match[1] || match[2];
        if (!explicitMoneyLabel.test(label)) continue;
        if (nonMoneyLabels.has(label)) continue;
        if (taxBasis.test(label) && moneyUnit.test(label)) continue;
        const line = source.slice(0, match.index).split("\n").length;
        ambiguous.push(`${file}:${line} ${label}`);
      }
    });

    expect(ambiguous).toEqual([]);
  });
});
