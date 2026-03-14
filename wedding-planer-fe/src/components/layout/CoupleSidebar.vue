<script setup lang="ts">
import { computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";
import { useCoupleStore } from "@/stores/couple.store";
import { useLeadsStore } from "@/stores/leads.store";
import { useMessagesStore } from "@/stores/messages.store";
import { useSidebar } from "@/composables/useSidebar";
import { setLocale } from "@/i18n";
import {
  LayoutDashboard,
  MessageSquare,
  CheckSquare,
  Wallet,
  Users,
  Search,
  Star,
  Video,
  Calendar,
  MessageCircle,
  Globe,
  FolderOpen,
  User,
} from "lucide-vue-next";

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const coupleStore = useCoupleStore();
const leadsStore = useLeadsStore();
const messagesStore = useMessagesStore();
const { sidebarOpen, close } = useSidebar();

onMounted(() => close());
watch(
  () => route.path,
  () => close(),
);

const pendingLeadsCount = computed(
  () => leadsStore.leads.filter((l) => l.status === "QUOTED").length,
);
const unreadCount = computed(() => messagesStore.totalUnread());

const navItems = computed(() => [
  {
    section: t("nav.couple.sections.planning"),
    items: [
      {
        label: t("nav.couple.items.overview"),
        path: "/couple/overview",
        icon: LayoutDashboard,
      },
      {
        label: t("nav.couple.items.myEnquiries"),
        path: "/couple/enquiries",
        icon: MessageSquare,
        badge: "enquiries",
      },
      {
        label: t("nav.couple.items.checklist"),
        path: "/couple/checklist",
        icon: CheckSquare,
      },
      {
        label: t("nav.couple.items.budgetTracker"),
        path: "/couple/budget",
        icon: Wallet,
      },
      {
        label: t("nav.couple.items.guestList"),
        path: "/couple/guests",
        icon: Users,
      },
    ],
  },
  {
    section: t("nav.couple.sections.vendors"),
    items: [
      {
        label: t("nav.couple.items.findVendors"),
        path: "/vendors",
        icon: Search,
      },
      {
        label: t("nav.couple.items.myWeddingTeam"),
        path: "/couple/team",
        icon: Star,
      },
      {
        label: t("nav.couple.items.videoCalls"),
        path: "/couple/calls",
        icon: Video,
      },
      {
        label: t("nav.couple.items.calendar"),
        path: "/couple/calendar",
        icon: Calendar,
      },
      {
        label: t("nav.couple.items.groupChat"),
        path: "/couple/group-chat",
        icon: MessageCircle,
      },
    ],
  },
  {
    section: t("nav.couple.sections.ourWedding"),
    items: [
      {
        label: t("nav.couple.items.weddingWebsite"),
        path: "/couple/website",
        icon: Globe,
      },
      {
        label: t("nav.couple.items.documentVault"),
        path: "/couple/documents",
        icon: FolderOpen,
      },
    ],
  },
  {
    section: t("nav.couple.sections.account"),
    items: [
      {
        label: t("nav.couple.items.myProfile"),
        path: "/couple/profile",
        icon: User,
      },
    ],
  },
]);

function isActive(path: string) {
  return route.path.startsWith(path);
}

function getBadge(key: string) {
  if (key === "enquiries") {
    const total = (pendingLeadsCount.value || 0) + (unreadCount.value || 0);
    return total || null;
  }
  return null;
}

function logout() {
  authStore.logout();
  router.push("/");
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
          v-if="coupleStore.profile?.profilePicture"
          :src="coupleStore.profile.profilePicture"
          class="avatar-img"
          alt=""
        />
        <template v-else>{{
          (coupleStore.profile?.partner1Name ??
            authStore.user?.email)?.[0]?.toUpperCase()
        }}</template>
      </div>
      <div class="user-name">
        <template v-if="coupleStore.profile?.partner1Name">
          {{ coupleStore.profile.partner1Name }}
          <template v-if="coupleStore.profile.partner2Name">
            &amp; {{ coupleStore.profile.partner2Name }}</template
          >
        </template>
        <template v-else>{{ authStore.user?.email }}</template>
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
          <span class="nav-icon"><component :is="item.icon" :size="16" /></span>
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="item.badge && getBadge(item.badge)" class="badge">
            {{ getBadge(item.badge) }}
          </span>
        </RouterLink>
      </div>
    </div>

    <button class="lang-btn" @click="switchLang" :title="t('lang.switch')">
      {{ locale === "ro" ? "EN" : "RO" }}
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
  position: relative;
  transition:
    transform 0.18s,
    box-shadow 0.18s;
}
.user-avatar:hover {
  transform: scale(3);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  z-index: 9999;
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
