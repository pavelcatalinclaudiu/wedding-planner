<script setup lang="ts">
import { computed, onMounted, onUnmounted } from "vue";
import { useRoute } from "vue-router";
import CoupleSidebar from "@/components/layout/CoupleSidebar.vue";
import Topbar from "@/components/layout/Topbar.vue";
import { useAuthStore } from "@/stores/auth.store";
import { useCoupleStore } from "@/stores/couple.store";
import { useLeadsStore } from "@/stores/leads.store";
import { useNotificationsStore } from "@/stores/notifications.store";
import { useWebSocket } from "@/composables/useWebSocket";
import { useNotificationSocket } from "@/composables/useNotificationSocket";
import { useTheme } from "@/composables/useTheme";

const route = useRoute();
const coupleStore = useCoupleStore();
const leadsStore = useLeadsStore();
const notificationsStore = useNotificationsStore();
const authStore = useAuthStore();
const ws = useWebSocket();
const notifSocket = useNotificationSocket();

const { isDark } = useTheme();

const pageTitle = computed(() => {
  return (route.meta.title as string) ?? "Dashboard";
});

onMounted(async () => {
  await Promise.all([
    coupleStore.fetchProfile(),
    leadsStore.fetchCoupleLeads(),
    notificationsStore.fetchNotifications(),
  ]);
  // Connect WS for each active lead
  for (const lead of leadsStore.activeLeads) {
    ws.connect(lead.id);
  }
  // Real-time notification delivery
  const userId = authStore.user?.id;
  if (userId) {
    notifSocket.connect(userId, () => notificationsStore.bumpUnread());
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
</style>
