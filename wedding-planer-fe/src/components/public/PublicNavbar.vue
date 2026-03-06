<script setup lang="ts">
import { computed } from "vue";
import { useAuthStore } from "@/stores/auth.store";
import NotificationBell from "@/components/notifications/NotificationBell.vue";

const authStore = useAuthStore();

const dashboardPath = computed(() =>
  authStore.user?.role === "COUPLE" ? "/couple/overview" : "/vendor/overview",
);
</script>

<template>
  <nav class="pub-nav">
    <RouterLink to="/" class="pub-nav-logo">Eternelle</RouterLink>

    <div class="pub-nav-links">
      <RouterLink to="/vendors" class="pub-nav-link">Find Vendors</RouterLink>
    </div>

    <div class="pub-nav-actions">
      <!-- Not logged in -->
      <template v-if="!authStore.isAuthenticated">
        <RouterLink to="/login" class="pub-nav-login">Log in</RouterLink>
        <RouterLink to="/register" class="pub-nav-cta">Get Started</RouterLink>
      </template>

      <!-- Logged in -->
      <template v-else>
        <RouterLink :to="dashboardPath" class="pub-nav-cta">
          My Dashboard
        </RouterLink>
        <NotificationBell />
      </template>
    </div>
  </nav>
</template>

<style scoped>
.pub-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  height: 64px;
  background: rgba(28, 28, 28, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.pub-nav-logo {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-gold, #c9a84c);
  text-decoration: none;
  letter-spacing: -0.5px;
}

.pub-nav-links {
  display: flex;
  gap: 28px;
}

.pub-nav-link {
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  transition: color 0.2s;
}

.pub-nav-link:hover {
  color: #fff;
}

.pub-nav-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.pub-nav-login {
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  transition: color 0.2s;
}

.pub-nav-login:hover {
  color: #fff;
}

.pub-nav-cta {
  background: var(--color-gold, #c9a84c);
  color: #fff;
  text-decoration: none;
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  transition: opacity 0.2s;
}

.pub-nav-cta:hover {
  opacity: 0.9;
}

@media (max-width: 640px) {
  .pub-nav {
    padding: 0 20px;
  }
  .pub-nav-links {
    display: none;
  }
}
</style>
