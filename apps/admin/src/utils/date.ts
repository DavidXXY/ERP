/**
 * Local-timezone date helpers.
 * Avoid `new Date().toISOString().slice(0, 10)` which returns the *UTC* date
 * and shifts a whole day for UTC+8 (and similar) zones during 00:00–07:59.
 */

function asDate(input: Date | string | number): Date {
  if (input instanceof Date) return input;
  return new Date(input);
}

/** Local YYYY-MM-DD for the given date (default: now). */
export function toLocalDateString(input: Date | string | number = new Date()): string {
  const d = asDate(input);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

/** Today's local YYYY-MM-DD. */
export function todayLocal(): string {
  return toLocalDateString(new Date());
}

/** Local YYYY-MM for the given date (default: now). */
export function toLocalMonthString(input: Date | string | number = new Date()): string {
  const d = asDate(input);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  return `${y}-${m}`;
}

/**
 * Local `YYYY-MM-DDTHH:mm` (or `YYYY-MM-DD HH:mm`) without UTC shift.
 * Prefer this over `.toISOString()` when sending a business datetime.
 */
export function toLocalDateTimeString(
  input: Date | string | number,
  separator: "T" | " " = "T",
): string {
  const d = asDate(input);
  const date = toLocalDateString(d);
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${date}${separator}${hh}:${mm}`;
}
