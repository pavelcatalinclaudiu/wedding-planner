<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";
import { useVendorStore } from "@/stores/vendor.store";
import { useLeadsStore } from "@/stores/leads.store";
import { useMessagesStore } from "@/stores/messages.store";
import { useSidebar } from "@/composables/useSidebar";
import { setLocale } from "@/i18n";

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const vendorStore = useVendorStore();
const leadsStore = useLeadsStore();
const messagesStore = useMessagesStore();
const { sidebarOpen, close } = useSidebar();

const unreadCount = computed(() => messagesStore.totalUnread());
const newLeadsCount = computed(() => leadsStore.newLeadsCount);

const navItems = computed(() => [
  {
    section: t("nav.vendor.sections.business"),
    items: [
      {
        label: t("nav.vendor.items.overview"),
        path: "/vendor/overview",
        icon: "⊞",
      },
      {
        label: t("nav.vendor.items.inbox"),
        path: "/vendor/leads",
        icon: "◈",
        badge: "leads",
      },
      {
        label: t("nav.vendor.items.availability"),
        path: "/vendor/calendar",
        icon: "◎",
      },
      {
        label: t("nav.vendor.items.videoCalls"),
        path: "/vendor/calls",
        icon: "⊙",
      },
    ],
  },
  {
    section: t("nav.vendor.sections.profile"),
    items: [
      {
        label: t("nav.vendor.items.portfolio"),
        path: "/vendor/portfolio",
        icon: "★",
      },
      {
        label: t("nav.vendor.items.reviews"),
        path: "/vendor/reviews",
        icon: "✦",
      },
      {
        label: t("nav.vendor.items.partnerNetwork"),
        path: "/vendor/network",
        icon: "⊗",
      },
      {
        label: t("nav.vendor.items.groupChat"),
        path: "/vendor/group-chat",
        icon: "⊙",
      },
    ],
  },
  {
    section: t("nav.vendor.sections.account"),
    items: [
      {
        label: t("nav.vendor.items.analytics"),
        path: "/vendor/analytics",
        icon: "≋",
      },
      {
        label: t("nav.vendor.items.subscription"),
        path: "/vendor/subscription",
        icon: "◇",
      },
      {
        label: t("nav.vendor.items.myProfile"),
        path: "/vendor/profile",
        icon: "◉",
      },
    ],
  },
]);

function isActive(path: string) {
  return route.path.startsWith(path);
}

function getBadge(key: string) {
  if (key === "leads") {
    const total = (newLeadsCount.value || 0) + (unreadCount.value || 0);
    return total || null;
  }
  return null;
}

onMounted(() => {
  if (!vendorStore.profile) vendorStore.fetchMyProfile();
});

function logout() {
  authStore.logout();
  router.push("/login");
}

function switchLang() {
  setLocale(locale.value === "ro" ? "en" : "ro");
}
</script>

<template>
  <div v-if="sidebarOpen" class="sidebar-backdrop" @click="close" />
  <nav class="sidebar" :class="{ 'sidebar--open': sidebarOpen }">
    <RouterLink to="/" class="sidebar-logo">Eternelle</RouterLink>

    <div class="user-info">
      <div class="user-avatar">
        <img
          v-if="vendorStore.profile?.coverPhoto"
          :src="vendorStore.profile.coverPhoto"
          class="avatar-img"
          alt=""
        />
        <span v-else>
          {{
            (vendorStore.profile?.businessName ??
              authStore.user?.email)?.[0]?.toUpperCase()
          }}
        </span>
      </div>
      <div class="user-name">
        {{ vendorStore.profile?.businessName ?? authStore.user?.email }}
      </div>
    </div>

    <div class="nav-sections" @click="close">
      <div
        v-for="section in navItems"
        :key="section.section"
        class="nav-section"
      >
        <p class="section-label">{{ section.section }}</p>
        <RouterLink
          v-for="item in section.items"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="item.badge && getBadge(item.badge)" class="badge">
            {{ getBadge(item.badge) }}
          </span>
        </RouterLink>
      </div>
    </div>

    <button class="lang-btn" @click="switchLang" :title="t('lang.switch')">
      {{ locale === "ro" ? "🇬🇧 EN" : "🇷🇴 RO" }}
    </button>
    <button class="logout-btn" @click="logout">
      {{ t("common.signOut") }}
    </button>
  </nav>
</template>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 240px;
  background: var(--color-white);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  padding: 24px 0;
  z-index: 100;
  overflow-y: auto;
}
.sidebar-logo {
  display: block;
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-gold);
  text-decoration: none;
  padding: 0 24px 20px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 16px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 24px 16px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 8px;
}
.user-avatar {
  width: 32px;
  height: 32px;
  background: var(--color-gold);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 700;
  flex-shrink: 0;
  overflow: hidden;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}
.user-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nav-sections {
  flex: 1;
  overflow-y: auto;
}
.nav-section {
  margin-bottom: 8px;
}
.section-label {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-muted);
  padding: 12px 24px 4px;
  margin: 0;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 24px;
  text-decoration: none;
  color: var(--color-text);
  font-size: 0.9rem;
  font-weight: 500;
  transition:
    background 0.15s,
    color 0.15s;
  position: relative;
}
.nav-item:hover {
  background: var(--color-surface);
}
.nav-item.active {
  background: var(--color-gold-light, #fdf8ee);
  color: var(--color-gold);
  font-weight: 600;
}
.nav-icon {
  width: 18px;
  text-align: center;
  font-style: normal;
  font-size: 0.95rem;
  flex-shrink: 0;
}
.nav-label {
  flex: 1;
}
.badge {
  background: var(--color-gold);
  color: #fff;
  border-radius: 10px;
  padding: 1px 6px;
  font-size: 0.7rem;
  font-weight: 700;
  min-width: 18px;
  text-align: center;
}
.logout-btn {
  margin: 8px 24px 0;
  padding: 8px;
  background: none;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--color-muted);
  cursor: pointer;
  transition:
    border-color 0.2s,
    color 0.2s;
}
.logout-btn:hover {
  border-color: var(--color-error);
  color: var(--color-error);
}
.lang-btn {
  margin: 16px 24px 0;
  padding: 7px;
  background: none;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.8rem;
  color: var(--color-muted);
  cursor: pointer;
  transition:
    border-color 0.2s,
    color 0.2s;
  text-align: center;
}
.lang-btn:hover {
  border-color: var(--color-gold);
  color: var(--color-gold);
}
@media (max-width: 767px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    z-index: 200;
  }
  .sidebar--open {
    transform: translateX(0);
  }
  .sidebar-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 199;
  }
}
</style>
