<script setup lang="ts">
import { computed, onMounted, onUnmounted } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import VendorSidebar from "@/components/layout/VendorSidebar.vue";
import Topbar from "@/components/layout/Topbar.vue";
import BottomTabBar from "@/components/layout/BottomTabBar.vue";
import { useVendorStore } from "@/stores/vendor.store";
import { useLeadsStore } from "@/stores/leads.store";
import { useMessagesStore } from "@/stores/messages.store";
import { useNotificationsStore } from "@/stores/notifications.store";
import { useAuthStore } from "@/stores/auth.store";
import { useNotificationSocket } from "@/composables/useNotificationSocket";
import { useTheme } from "@/composables/useTheme";
import {
  LayoutDashboard,
  Inbox,
  CalendarDays,
  Image as ImageIcon,
  User,
} from "lucide-vue-next";

const { t } = useI18n();
const route = useRoute();
const vendorStore = useVendorStore();
const leadsStore = useLeadsStore();
const messagesStore = useMessagesStore();
const notificationsStore = useNotificationsStore();
const authStore = useAuthStore();
const notifSocket = useNotificationSocket();

const { isDark } = useTheme();

const pageTitle = computed(() => {
  const key = route.meta.title as string | undefined;
  return key ? t(key) : "Dashboard";
});

const inboxBadge = computed(() => {
  const leads = leadsStore.newLeadsCount || 0;
  const msgs = messagesStore.totalUnread() || 0;
  return leads + msgs || null;
});

const bottomTabs = computed(() => [
  {
    label: t("nav.vendor.items.overview"),
    path: "/vendor/overview",
    icon: LayoutDashboard,
  },
  {
    label: t("nav.vendor.items.inbox"),
    path: "/vendor/leads",
    icon: Inbox,
    badge: inboxBadge.value,
  },
  {
    label: t("nav.vendor.items.availability"),
    path: "/vendor/calendar",
    icon: CalendarDays,
  },
  {
    label: t("nav.vendor.items.portfolio"),
    path: "/vendor/portfolio",
    icon: ImageIcon,
  },
  {
    label: t("nav.vendor.items.myProfile"),
    path: "/vendor/profile",
    icon: User,
  },
]);

onMounted(async () => {
  await Promise.all([
    vendorStore.fetchMyProfile(),
    leadsStore.fetchVendorLeads(),
    notificationsStore.fetchNotifications(),
  ]);
  const userId = authStore.user?.id;
  if (userId) {
    notifSocket.connect(userId, () => notificationsStore.bumpUnread());
  }
});

onUnmounted(() => notifSocket.disconnect());
</script>

<template>
  <div class="vendor-layout" :class="{ dark: isDark }">
    <VendorSidebar />
    <div class="layout-body">
      <Topbar :title="pageTitle" />
      <main class="main-content">
        <RouterView />
      </main>
    </div>
    <BottomTabBar :tabs="bottomTabs" />
  </div>
</template>

<style scoped>
.vendor-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--color-surface);
  color: var(--color-text);
}
.layout-body {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
@media (max-width: 767px) {
  .layout-body {
    margin-left: 0;
  }
}
.main-content {
  flex: 1;
  padding: 28px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
@media (max-width: 767px) {
  .main-content {
    padding: 16px 16px calc(60px + env(safe-area-inset-bottom, 0) + 16px);
  }
}
</style>
