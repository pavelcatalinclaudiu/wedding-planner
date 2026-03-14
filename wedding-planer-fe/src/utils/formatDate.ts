/**
 * Shared date/time formatting utilities used across the app.
 * Wraps Intl.DateTimeFormat so all dates are consistently localised.
 */

type Locale = "en" | "ro";

function intlLocale(locale: Locale) {
  return locale === "ro" ? "ro-RO" : "en-GB";
}

/**
 * "March 14, 2026"
 */
export function formatDate(
  date: string | Date | null | undefined,
  locale: Locale = "en",
): string {
  if (!date) return "";
  const d = typeof date === "string" ? new Date(date) : date;
  if (isNaN(d.getTime())) return "";
  return d.toLocaleDateString(intlLocale(locale), {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

/**
 * "14 Mar 2026"
 */
export function formatDateShort(
  date: string | Date | null | undefined,
  locale: Locale = "en",
): string {
  if (!date) return "";
  const d = typeof date === "string" ? new Date(date) : date;
  if (isNaN(d.getTime())) return "";
  return d.toLocaleDateString(intlLocale(locale), {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

/**
 * "14 Mar"
 */
export function formatDateMonthDay(
  date: string | Date | null | undefined,
  locale: Locale = "en",
): string {
  if (!date) return "";
  const d = typeof date === "string" ? new Date(date) : date;
  if (isNaN(d.getTime())) return "";
  return d.toLocaleDateString(intlLocale(locale), {
    day: "numeric",
    month: "short",
  });
}

/**
 * "10:30"
 */
export function formatTime(
  date: string | Date | null | undefined,
  locale: Locale = "en",
): string {
  if (!date) return "";
  const d = typeof date === "string" ? new Date(date) : date;
  if (isNaN(d.getTime())) return "";
  return d.toLocaleTimeString(intlLocale(locale), {
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * "14 Mar 2026, 10:30"
 */
export function formatDateTime(
  date: string | Date | null | undefined,
  locale: Locale = "en",
): string {
  if (!date) return "";
  const d = typeof date === "string" ? new Date(date) : date;
  if (isNaN(d.getTime())) return "";
  return d.toLocaleString(intlLocale(locale), {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
