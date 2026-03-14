<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";
import { setLocale } from "@/i18n";
import NotificationBell from "@/components/notifications/NotificationBell.vue";
import { LayoutDashboard, User } from "lucide-vue-next";

const { t, locale } = useI18n();
const authStore = useAuthStore();

const dashboardPath = computed(() =>
  authStore.user?.role === "COUPLE" ? "/couple/overview" : "/vendor/overview",
);

function switchTo(lang: "ro" | "en") {
  if (locale.value !== lang) setLocale(lang);
}

const authMenuOpen = ref(false);
function toggleAuthMenu() {
  authMenuOpen.value = !authMenuOpen.value;
}
function closeAuthMenu() {
  authMenuOpen.value = false;
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
        <!-- Desktop: text links -->
        <RouterLink to="/login" class="pub-nav-login">
          {{ t("pubNav.logIn") }}
        </RouterLink>
        <RouterLink to="/register" class="pub-nav-cta pub-nav-cta-desktop">
          {{ t("pubNav.getStarted") }}
        </RouterLink>

        <!-- Mobile: user icon with dropdown -->
        <div class="auth-menu-wrap">
          <button
            class="auth-icon-btn"
            @click="toggleAuthMenu"
            :aria-expanded="authMenuOpen"
          >
            <User :size="20" />
          </button>
          <div v-if="authMenuOpen" class="auth-dropdown" @click="closeAuthMenu">
            <RouterLink to="/login" class="auth-dropdown-item">
              {{ t("pubNav.logIn") }}
            </RouterLink>
            <RouterLink
              to="/register"
              class="auth-dropdown-item auth-dropdown-cta"
            >
              {{ t("pubNav.getStarted") }}
            </RouterLink>
          </div>
        </div>
      </template>

      <!-- Logged in -->
      <template v-else>
        <RouterLink :to="dashboardPath" class="pub-nav-cta">
          <LayoutDashboard :size="17" class="cta-icon" />
          <span class="cta-label">{{ t("pubNav.myDashboard") }}</span>
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
  font-family: inherit;
}

/* ── Logo (left) ── */
.pub-nav-logo {
  font-family: "Cinzel", Georgia, serif;
  font-size: 1.45rem;
  font-weight: 600;
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
  font-family: inherit;
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
  display: flex;
  align-items: center;
  gap: 7px;
}

.pub-nav-cta:hover {
  opacity: 0.88;
}

/* ── Bell icon: force white on dark navbar ── */
:deep(.bell-btn) {
  color: rgba(255, 255, 255, 0.85);
}
:deep(.bell-btn:hover),
:deep(.bell-btn.active) {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
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
  .pub-nav-cta-desktop {
    display: none;
  }
  .pub-nav-cta {
    padding: 8px 10px;
  }
  .cta-label {
    display: none;
  }
}

/* On desktop, hide the mobile auth icon */
@media (min-width: 641px) {
  .auth-menu-wrap {
    display: none;
  }
}

/* ── Auth icon + dropdown ── */
.auth-menu-wrap {
  position: relative;
}

.auth-icon-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.85);
  border-radius: 8px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s;
}
.auth-icon-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.auth-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  background: #1c1c1c;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  overflow: hidden;
  min-width: 160px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  z-index: 200;
}

.auth-dropdown-item {
  display: block;
  padding: 13px 18px;
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  transition: background 0.15s;
  text-align: center;
}
.auth-dropdown-item:hover {
  background: rgba(255, 255, 255, 0.07);
  color: #fff;
}
.auth-dropdown-cta {
  background: var(--color-gold, #c9a84c);
  color: #fff;
  font-weight: 700;
  text-align: center;
}
.auth-dropdown-cta:hover {
  background: #b8963a;
  color: #fff;
}
</style>
