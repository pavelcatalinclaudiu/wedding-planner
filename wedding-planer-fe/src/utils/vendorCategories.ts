import { useI18n } from "vue-i18n";

/** Maps English display name (stored in DB) → VendorCategory enum key */
export const VENDOR_CATEGORY_MAP: Record<string, string> = {
  Photographer: "PHOTOGRAPHER",
  Videographer: "VIDEOGRAPHER",
  Florist: "FLORIST",
  Caterer: "CATERER",
  Venue: "VENUE",
  "Live Band": "BAND",
  DJ: "DJ",
  Cake: "CAKE",
  "Makeup Artist": "MAKEUP_ARTIST",
  "Hair Stylist": "HAIR_STYLIST",
  Officiant: "OFFICIANT",
  "Wedding Planner": "PLANNER",
  Transportation: "TRANSPORTATION",
  Lighting: "LIGHTING",
  "Invitations & Stationery": "INVITATION_STATIONERY",
  Jewelry: "JEWELRY",
  Other: "OTHER",
};

/**
 * Translates a stored category string (English display name) into the current locale.
 * Falls back to the raw stored value for custom user-entered categories.
 */
export function useCategoryLabel() {
  const { t, te } = useI18n();

  function categoryLabel(cat: string): string {
    const key = VENDOR_CATEGORY_MAP[cat];
    if (key && te(`categories.${key}`)) return t(`categories.${key}`);
    return cat;
  }

  return { categoryLabel };
}
