<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import CoupleSidebar from "@/components/layout/CoupleSidebar.vue";
import Topbar from "@/components/layout/Topbar.vue";
import BottomTabBar from "@/components/layout/BottomTabBar.vue";
import WelcomeOnboarding from "@/components/couple/WelcomeOnboarding.vue";
import { useAuthStore } from "@/stores/auth.store";
import { useCoupleStore } from "@/stores/couple.store";
import { useLeadsStore } from "@/stores/leads.store";
import { useMessagesStore } from "@/stores/messages.store";
import { useNotificationsStore } from "@/stores/notifications.store";
import { useNotificationSocket } from "@/composables/useNotificationSocket";
import { useTheme } from "@/composables/useTheme";
import {
  LayoutDashboard,
  MessageSquare,
  CheckSquare,
  Search,
  MessageCircle,
} from "lucide-vue-next";

const { t } = useI18n();
const route = useRoute();
const coupleStore = useCoupleStore();
const leadsStore = useLeadsStore();
const messagesStore = useMessagesStore();
const notificationsStore = useNotificationsStore();
const authStore = useAuthStore();
const notifSocket = useNotificationSocket();

const { isDark } = useTheme();

const showOnboarding = ref(false);
const ONBOARDING_KEY = computed(
  () => `onboarding_completed_${authStore.user?.id ?? "guest"}`,
);

const pageTitle = computed(() => {
  const key = route.meta.title as string | undefined;
  return key ? t(key) : "Dashboard";
});

const enquiriesBadge = computed(() => {
  const leads = leadsStore.leads.filter((l) => l.status === "QUOTED").length;
  const msgs = messagesStore.totalUnread();
  return leads + msgs || null;
});

const bottomTabs = computed(() => [
  {
    label: t("nav.couple.items.overview"),
    path: "/couple/overview",
    icon: LayoutDashboard,
  },
  {
    label: t("nav.couple.items.myEnquiries"),
    path: "/couple/enquiries",
    icon: MessageSquare,
    badge: enquiriesBadge.value,
  },
  {
    label: t("nav.couple.items.checklist"),
    path: "/couple/checklist",
    icon: CheckSquare,
  },
  { label: t("nav.couple.items.findVendors"), path: "/vendors", icon: Search },
  {
    label: t("nav.couple.items.groupChat"),
    path: "/couple/group-chat",
    icon: MessageCircle,
  },
]);

onMounted(async () => {
  await Promise.all([
    coupleStore.fetchProfile(),
    leadsStore.fetchCoupleLeads(),
    notificationsStore.fetchNotifications(),
  ]);
  const userId = authStore.user?.id;
  if (userId) {
    notifSocket.connect(userId, () => notificationsStore.bumpUnread());
  }
  // Show onboarding for first-time users
  if (!localStorage.getItem(ONBOARDING_KEY.value)) {
    setTimeout(() => {
      showOnboarding.value = true;
    }, 800);
  }
});

onUnmounted(() => notifSocket.disconnect());
</script>

<template>
  <div class="couple-layout" :class="{ dark: isDark }">
    <CoupleSidebar />
    <div class="layout-body">
      <Topbar :title="pageTitle" />
      <main class="main-content">
        <RouterView />
      </main>
    </div>
    <BottomTabBar :tabs="bottomTabs" />
    <WelcomeOnboarding v-if="showOnboarding" @close="showOnboarding = false" />
  </div>
</template>

<style scoped>
.couple-layout {
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
/* Extra bottom padding on mobile so content isn't hidden behind tab bar */
@media (max-width: 767px) {
  .main-content {
    padding: 16px 16px calc(60px + env(safe-area-inset-bottom, 0) + 16px);
  }
}
</style>
