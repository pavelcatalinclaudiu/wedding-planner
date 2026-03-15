<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";
import { useAdminStore } from "@/stores/admin.store";
import { useTheme } from "@/composables/useTheme";
import { setLocale } from "@/i18n";
import {
  LayoutDashboard,
  Users,
  Store,
  Star,
  LogOut,
  Sun,
  Moon,
  Menu,
  X,
  ShieldCheck,
} from "lucide-vue-next";

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const adminStore = useAdminStore();
const { isDark, toggleDark } = useTheme();

const sidebarOpen = ref(false);

const pageTitle = computed(() => {
  const key = route.meta.title as string | undefined;
  return key ? t(key) : "Admin";
});

const pendingReviews = computed(
  () => adminStore.stats?.pendingReviewsCount ?? 0,
);

const navItems = [
  {
    label: computed(() => t("admin.nav.overview")),
    path: "/admin/overview",
    icon: LayoutDashboard,
    badge: computed(() => 0),
  },
  {
    label: computed(() => t("admin.nav.users")),
    path: "/admin/users",
    icon: Users,
    badge: computed(() => 0),
  },
  {
    label: computed(() => t("admin.nav.vendors")),
    path: "/admin/vendors",
    icon: Store,
    badge: computed(() => 0),
  },
  {
    label: computed(() => t("admin.nav.reviews")),
    path: "/admin/reviews",
    icon: Star,
    badge: computed(() => pendingReviews.value),
  },
];

function isActive(path: string) {
  return route.path.startsWith(path);
}

function logout() {
  authStore.logout();
  router.push("/");
}

function switchLang() {
  setLocale(locale.value === "ro" ? "en" : "ro");
}

// Pre-load stats so the pending-reviews badge is immediately visible
if (!adminStore.stats) adminStore.fetchStats();
</script>

<template>
  <div class="admin-layout" :class="{ dark: isDark }">
    <!-- Backdrop for mobile -->
    <div
      v-if="sidebarOpen"
      class="sidebar-backdrop"
      @click="sidebarOpen = false"
    />

    <!-- Sidebar -->
    <nav class="admin-sidebar" :class="{ 'sidebar--open': sidebarOpen }">
      <RouterLink to="/" class="sidebar-logo">
        <ShieldCheck :size="18" />
        Eternelle Admin
      </RouterLink>

      <div class="user-info">
        <div class="user-avatar">
          {{ authStore.user?.email?.[0]?.toUpperCase() }}
        </div>
        <div class="user-details">
          <div class="user-email">{{ authStore.user?.email }}</div>
          <div class="user-role-badge">ADMIN</div>
        </div>
      </div>

      <div class="nav-list" @click="sidebarOpen = false">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <component :is="item.icon" :size="16" />
          <span>{{ item.label.value }}</span>
          <span v-if="item.badge.value > 0" class="nav-badge">{{
            item.badge.value
          }}</span>
        </RouterLink>
      </div>

      <div class="sidebar-footer">
        <button class="lang-btn" @click="switchLang" :title="t('lang.switch')">
          {{ locale === "ro" ? "EN" : "RO" }}
        </button>
        <button class="theme-btn" @click="toggleDark">
          <Sun v-if="isDark" :size="16" />
          <Moon v-else :size="16" />
        </button>
        <button class="logout-btn" @click="logout">
          <LogOut :size="16" />
          {{ t("common.signOut") }}
        </button>
      </div>
    </nav>

    <!-- Main content -->
    <div class="layout-body">
      <header class="admin-topbar">
        <button class="hamburger" @click="sidebarOpen = !sidebarOpen">
          <X v-if="sidebarOpen" :size="20" />
          <Menu v-else :size="20" />
        </button>
        <h1 class="topbar-title">{{ pageTitle }}</h1>
      </header>

      <main class="main-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--color-surface);
  color: var(--color-text);
}

/* ── Sidebar ──────────────────────────────────────────── */
.admin-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: var(--color-card);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  padding: 0 0 16px;
  z-index: 100;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-gold);
  padding: 22px 20px 18px;
  text-decoration: none;
  letter-spacing: 0.5px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 8px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-gold);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-details {
  min-width: 0;
}

.user-email {
  font-size: 12px;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
}

.user-role-badge {
  display: inline-block;
  margin-top: 2px;
  font-size: 10px;
  font-weight: 700;
  color: var(--color-gold);
  background: rgba(212, 175, 55, 0.12);
  border-radius: 4px;
  padding: 1px 6px;
  letter-spacing: 1px;
}

.nav-list {
  flex: 1;
  padding: 4px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  color: var(--color-text-muted);
  text-decoration: none;
  font-size: 14px;
  transition:
    background 0.15s,
    color 0.15s;
}

.nav-item:hover {
  background: var(--color-surface);
  color: var(--color-text);
}

.nav-item.active {
  background: rgba(212, 175, 55, 0.12);
  color: var(--color-gold);
  font-weight: 600;
}

.nav-badge {
  margin-left: auto;
  background: #e53e3e;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  border-radius: 20px;
  padding: 1px 6px;
  min-width: 18px;
  text-align: center;
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px 0;
  border-top: 1px solid var(--color-border);
  margin-top: 8px;
}

.lang-btn,
.theme-btn {
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text-muted);
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 12px;
  cursor: pointer;
  transition:
    color 0.15s,
    border-color 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lang-btn:hover,
.theme-btn:hover {
  color: var(--color-text);
  border-color: var(--color-text-muted);
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-left: auto;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 13px;
  padding: 4px 6px;
  border-radius: 6px;
  transition: color 0.15s;
}

.logout-btn:hover {
  color: #e53e3e;
}

/* ── Main body ────────────────────────────────────────── */
.layout-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.admin-topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 28px;
  height: 56px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-card);
  flex-shrink: 0;
}

.hamburger {
  display: none;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-text);
  padding: 4px;
}

.topbar-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.main-content {
  flex: 1;
  padding: 28px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* ── Backdrop ─────────────────────────────────────────── */
.sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 99;
}

/* ── Mobile ───────────────────────────────────────────── */
@media (max-width: 767px) {
  .admin-sidebar {
    position: fixed;
    top: 0;
    left: -260px;
    height: 100%;
    transition: left 0.25s ease;
  }
  .admin-sidebar.sidebar--open {
    left: 0;
  }
  .layout-body {
    margin-left: 0;
  }
  .hamburger {
    display: flex;
  }
  .main-content {
    padding: 16px;
  }
}
</style>
