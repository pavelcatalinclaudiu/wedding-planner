/**
 * Generates deterministic background / text colour pairs from a string.
 * Useful for avatar placeholders where no profile picture is set.
 */

const PALETTE: Array<{ bg: string; text: string }> = [
  { bg: "#fde8d8", text: "#c8974a" }, // gold
  { bg: "#d8f0e8", text: "#2d7a55" }, // green
  { bg: "#d8e8fd", text: "#2d55a0" }, // blue
  { bg: "#f0d8fd", text: "#7c3aed" }, // purple
  { bg: "#fdd8d8", text: "#c0392b" }, // red
  { bg: "#d8f8fd", text: "#0891b2" }, // cyan
  { bg: "#fdf0d8", text: "#b45309" }, // amber
  { bg: "#f0d8f0", text: "#9d174d" }, // pink
  { bg: "#dcfce7", text: "#15803d" }, // emerald
  { bg: "#fef9c3", text: "#a16207" }, // yellow
];

/**
 * Returns a `{ bg, text }` colour pair derived from the given name.
 * The result is stable — the same name always produces the same colour.
 */
export function nameToColor(name: string): { bg: string; text: string } {
  if (!name) return PALETTE[0];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return PALETTE[Math.abs(hash) % PALETTE.length];
}

/**
 * Returns the initials to display inside an avatar (up to 2 characters).
 * e.g. "Ana Pop" → "AP", "Maria" → "M"
 */
export function nameToInitials(name: string): string {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0][0].toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}
