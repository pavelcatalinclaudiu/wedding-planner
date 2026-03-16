import { computed } from "vue";
import { useVendorStore } from "@/stores/vendor.store";
import { useCoupleStore } from "@/stores/couple.store";
import { useAuthStore } from "@/stores/auth.store";

const VENDOR_FEATURES: Record<string, string[]> = {
  videoCalls: ["FREE", "STANDARD", "PREMIUM"],
  network: ["STANDARD", "PREMIUM"],
  analytics: ["STANDARD", "PREMIUM"],
  leadExport: ["PREMIUM"],
};

const COUPLE_FEATURES: Record<string, string[]> = {
  videoCalls: ["FREE", "DREAM_WEDDING"],
  documents: ["DREAM_WEDDING"],
};

export function useFeatureAccess() {
  const authStore = useAuthStore();
  const vendorStore = useVendorStore();
  const coupleStore = useCoupleStore();

  const role = computed(() => authStore.user?.role);

  const monetizationEnabled = computed(() => {
    if (role.value === "VENDOR")
      return vendorStore.profile?.monetizationEnabled ?? false;
    if (role.value === "COUPLE")
      return coupleStore.profile?.monetizationEnabled ?? false;
    return false;
  });

  function canAccess(feature: string): boolean {
    if (!monetizationEnabled.value) return true;
    if (role.value === "VENDOR") {
      const tier = (vendorStore.profile?.tier as string) ?? "FREE";
      const required = VENDOR_FEATURES[feature];
      return !required || required.includes(tier);
    }
    if (role.value === "COUPLE") {
      const plan = (coupleStore.profile?.plan as string) ?? "FREE";
      const required = COUPLE_FEATURES[feature];
      return !required || required.includes(plan);
    }
    return true;
  }

  const upgradePath = computed(() =>
    role.value === "VENDOR" ? "/vendor/subscription" : "/couple/subscription",
  );

  return { canAccess, monetizationEnabled, upgradePath };
}
