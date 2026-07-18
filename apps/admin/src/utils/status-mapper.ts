/**
 * Type-safe status label/color mapper utility.
 * Replaces inline `{KEY:val}[value]||default` patterns that cause TS7053.
 *
 * Usage in templates:
 *   <a-tag :color="statusColor(record.status, { DRAFT:'default', ... })">
 *     {{ statusLabel(record.status, { DRAFT:'草稿', ... }) }}
 *   </a-tag>
 */

export function statusLabel(
  value: string | undefined | null,
  labels: Record<string, string>,
  defaultLabel?: string,
): string {
  if (value == null) return defaultLabel ?? "";
  return labels[value] ?? defaultLabel ?? value;
}

export function statusColor(
  value: string | undefined | null,
  colors: Record<string, string>,
  defaultColor?: string,
): string {
  if (value == null) return defaultColor ?? "default";
  return colors[value] ?? defaultColor ?? "default";
}
