<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";
import { useVendorStore } from "@/stores/vendor.store";
import { useTheme } from "@/composables/useTheme";
import { useSidebar } from "@/composables/useSidebar";
import NotificationBell from "@/components/notifications/NotificationBell.vue";
import DashboardSearchBar from "@/components/couple/DashboardSearchBar.vue";
import { vendorApi } from "@/api/vendor.api";
import { Sun, Moon } from "lucide-vue-next";

const { t } = useI18n();
defineProps<{ title: string }>();

const { isDark, toggleDark } = useTheme();
const { toggle: toggleSidebar } = useSidebar();

const authStore = useAuthStore();
const vendorStore = useVendorStore();
const isCouple = computed(() => authStore.user?.role === "COUPLE");
const isVendor = computed(() => authStore.user?.role === "VENDOR");
const isLive = computed(() => vendorStore.profile?.active ?? false);

async function toggleVisibility() {
  const res = await vendorApi.toggleVisibility();
  if (vendorStore.profile) {
    vendorStore.profile.active = res.data.active;
  }
}
</script>

<template>
  <header class="topbar">
    <button
      class="hamburger"
      @click="toggleSidebar"
      :aria-label="t('topbar.openMenu')"
    >
      <span></span><span></span><span></span>
    </button>

    <!-- Vendor search — only shown inside couple dashboard -->
    <DashboardSearchBar v-if="isCouple" class="topbar-search" />

    <div class="topbar-right">
      <div v-if="isVendor" class="chip-wrap">
        <div
          class="profile-live-chip"
          :class="{ offline: !isLive }"
          @click="toggleVisibility"
        >
          <span class="live-dot"></span>
          {{ isLive ? t("topbar.profileLive") : t("topbar.profileHidden") }}
        </div>
        <div class="chip-tooltip">
          <template v-if="isLive">
            <strong>{{ t("topbar.profileLiveTooltip") }}</strong
            ><br />
            <em>{{ t("topbar.clickToHide") }}</em>
          </template>
          <template v-else>
            <strong>{{ t("topbar.profileHiddenTooltip") }}</strong
            ><br />
            <em>{{ t("topbar.clickToShow") }}</em>
          </template>
        </div>
      </div>
      <button
        class="theme-toggle"
        :title="isDark ? t('topbar.switchToLight') : t('topbar.switchToDark')"
        @click="toggleDark"
      >
        <Sun v-if="isDark" :size="18" />
        <Moon v-else :size="18" />
      </button>
      <NotificationBell />
    </div>
  </header>
</template>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  height: 81px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 28px;
  gap: 16px;
  z-index: 50;
}
.hamburger {
  display: none;
  flex-direction: column;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 2px;
  flex-shrink: 0;
}
.hamburger span {
  display: block;
  width: 22px;
  height: 2px;
  background: var(--color-text);
  border-radius: 2px;
  transition: background 0.2s;
}
.hamburger:hover span {
  background: var(--color-gold);
}
@media (max-width: 767px) {
  .hamburger {
    display: flex;
  }

  .topbar {
    padding: 0 16px;
    height: 64px;
    justify-content: space-between;
  }

  /* Hide the search bar on mobile — frees up topbar space */
  .topbar-search {
    display: none;
  }

  /* Shrink spacing between right-side icons */
  .topbar-right {
    gap: 8px;
  }

  /* Hide the "live/offline" chip text on mobile, keep just the dot */
  .profile-live-chip {
    padding: 4px 10px;
    font-size: 0.75rem;
  }
}
.topbar-search {
  flex: 1;
  max-width: 360px;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.chip-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.chip-tooltip {
  display: none;
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 230px;
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 0.8rem;
  line-height: 1.55;
  color: var(--color-text);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  z-index: 200;
  pointer-events: none;
}
.chip-tooltip::before {
  content: "";
  position: absolute;
  top: -6px;
  right: 20px;
  width: 10px;
  height: 10px;
  background: var(--color-white);
  border-left: 1px solid var(--color-border);
  border-top: 1px solid var(--color-border);
  transform: rotate(45deg);
}
.chip-wrap:hover .chip-tooltip {
  display: block;
}
.profile-live-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--chip-green-bg);
  color: var(--color-green);
  border: 1px solid var(--color-green);
  border-radius: 20px;
  padding: 4px 14px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  user-select: none;
  transition:
    background 0.2s,
    color 0.2s,
    border-color 0.2s;
}
.profile-live-chip.offline {
  background: var(--color-surface);
  color: var(--color-muted);
  border-color: var(--color-border);
}
.profile-live-chip.offline .live-dot {
  background: var(--color-muted);
  animation: none;
}
.theme-toggle {
  background: none;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  cursor: pointer;
  color: var(--color-text);
  padding: 0;
  transition:
    border-color 0.2s,
    background 0.2s;
  flex-shrink: 0;
}
.theme-toggle:hover {
  border-color: var(--color-gold);
  background: var(--color-gold-light);
}
.live-dot {
  width: 7px;
  height: 7px;
  background: var(--color-green, #27ae60);
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}
</style>
