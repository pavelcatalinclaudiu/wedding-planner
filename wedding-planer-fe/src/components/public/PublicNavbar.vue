<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";
import { setLocale } from "@/i18n";
import NotificationBell from "@/components/notifications/NotificationBell.vue";

const { t, locale } = useI18n();
const authStore = useAuthStore();

const dashboardPath = computed(() =>
  authStore.user?.role === "COUPLE" ? "/couple/overview" : "/vendor/overview",
);

function switchTo(lang: "ro" | "en") {
  if (locale.value !== lang) setLocale(lang);
}
</script>

<template>
  <nav class="pub-nav">
    <!-- Left: Logo -->
    <RouterLink to="/" class="pub-nav-logo">Eternelle</RouterLink>

    <!-- Center: primary nav link -->
    <div class="pub-nav-center">
      <RouterLink to="/vendors" class="pub-nav-link">
        {{ t("pubNav.findVendors") }}
      </RouterLink>
    </div>

    <!-- Right: language + auth -->
    <div class="pub-nav-actions">
      <!-- Language switcher -->
      <div class="lang-switcher" :aria-label="t('pubNav.switchLanguage')">
        <button
          class="lang-opt"
          :class="{ active: locale === 'ro' }"
          @click="switchTo('ro')"
        >
          RO
        </button>
        <span class="lang-sep">|</span>
        <button
          class="lang-opt"
          :class="{ active: locale === 'en' }"
          @click="switchTo('en')"
        >
          EN
        </button>
      </div>

      <!-- Not logged in -->
      <template v-if="!authStore.isAuthenticated">
        <RouterLink to="/login" class="pub-nav-login">
          {{ t("pubNav.logIn") }}
        </RouterLink>
        <RouterLink to="/register" class="pub-nav-cta">
          {{ t("pubNav.getStarted") }}
        </RouterLink>
      </template>

      <!-- Logged in -->
      <template v-else>
        <RouterLink :to="dashboardPath" class="pub-nav-cta">
          {{ t("pubNav.myDashboard") }}
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
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 48px;
  height: 64px;
  background: rgba(28, 28, 28, 0.88);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

/* ── Logo (left) ── */
.pub-nav-logo {
  font-size: 1.45rem;
  font-weight: 800;
  color: var(--color-gold, #c9a84c);
  text-decoration: none;
  letter-spacing: -0.5px;
  justify-self: start;
}

/* ── Center nav ── */
.pub-nav-center {
  display: flex;
  justify-content: center;
}

.pub-nav-link {
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  letter-spacing: 0.02em;
  padding: 6px 0;
  border-bottom: 2px solid transparent;
  transition:
    color 0.2s,
    border-color 0.2s;
}

.pub-nav-link:hover,
.pub-nav-link.router-link-active {
  color: #fff;
  border-bottom-color: var(--color-gold, #c9a84c);
}

/* ── Right actions ── */
.pub-nav-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  justify-self: end;
}

/* ── Language switcher ── */
.lang-switcher {
  display: flex;
  align-items: center;
  gap: 2px;
  background: rgba(255, 255, 255, 0.07);
  border-radius: 8px;
  padding: 3px 6px;
}

.lang-opt {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.45);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  transition: color 0.15s;
  line-height: 1;
}

.lang-opt:hover {
  color: rgba(255, 255, 255, 0.8);
}

.lang-opt.active {
  color: var(--color-gold, #c9a84c);
}

.lang-sep {
  color: rgba(255, 255, 255, 0.18);
  font-size: 0.7rem;
  user-select: none;
}

/* ── Auth links ── */
.pub-nav-login {
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 0.875rem;
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
  white-space: nowrap;
}

.pub-nav-cta:hover {
  opacity: 0.88;
}

/* ── Mobile ── */
@media (max-width: 640px) {
  .pub-nav {
    padding: 0 20px;
    grid-template-columns: 1fr auto;
  }
  .pub-nav-center {
    display: none;
  }
  .pub-nav-login {
    display: none;
  }
}
</style>
