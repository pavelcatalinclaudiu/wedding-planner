import { computed } from "vue";
import { useColorMode } from "@vueuse/core";

/**
 * Module-level singleton so every component shares the same reactive state.
 *
 * `onChanged` is a no-op: VueUse would normally apply the `.dark` class to
 * `<html>`, which would bleed into auth/public pages.  Instead we apply the
 * class manually on the layout root divs (VendorLayout / CoupleLayout) so
 * dark mode is scoped to dashboards only.
 */
const colorMode = useColorMode({
  storageKey: "eternelle-theme",
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onChanged() {},
});

export function useTheme() {
  const isDark = computed(() => colorMode.value === "dark");

  function toggleDark() {
    colorMode.value = isDark.value ? "light" : "dark";
  }

  return { isDark, toggleDark };
}
