export const sourcingMethodOptions = [
  { label: "竞争性询价", value: "COMPETITIVE" },
  { label: "单一来源", value: "SINGLE_SOURCE" },
  { label: "框架协议", value: "FRAMEWORK" },
] as const;

export const sourcingMethodLabels: Record<string, string> = Object.fromEntries(
  sourcingMethodOptions.map(({ label, value }) => [value, label]),
);

export function sourcingMethodLabel(value?: string | null) {
  if (!value) return "-";
  return sourcingMethodLabels[value] || value;
}
