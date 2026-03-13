<script setup lang="ts">
import { type Component } from "vue";
import { useRoute } from "vue-router";
import { RouterLink } from "vue-router";

export interface TabItem {
  label: string;
  path: string;
  icon: Component;
  badge?: number | null;
}

const props = defineProps<{ tabs: TabItem[] }>();
const route = useRoute();

function isActive(path: string) {
  return route.path.startsWith(path);
}
</script>

<template>
  <nav class="bottom-tab-bar">
    <RouterLink
      v-for="tab in props.tabs"
      :key="tab.path"
      :to="tab.path"
      class="tab-item"
      :class="{ active: isActive(tab.path) }"
    >
      <span class="tab-icon-wrap">
        <component :is="tab.icon" :size="22" />
        <span v-if="tab.badge" class="tab-badge">{{
          tab.badge > 9 ? "9+" : tab.badge
        }}</span>
      </span>
      <span class="tab-label">{{ tab.label }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.bottom-tab-bar {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: var(--color-white);
  border-top: 1px solid var(--color-border);
  z-index: 150;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.07);
  /* Safe area support for iPhone */
  padding-bottom: env(safe-area-inset-bottom, 0);
}
@media (max-width: 767px) {
  .bottom-tab-bar {
    display: flex;
    align-items: stretch;
  }
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  text-decoration: none;
  color: var(--color-muted);
  font-size: 0.65rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  transition: color var(--transition-fast);
  padding: 6px 2px 4px;
  position: relative;
}
.tab-item:hover {
  text-decoration: none;
  color: var(--color-text);
}
.tab-item.active {
  color: var(--color-gold);
}
.tab-item.active::before {
  content: "";
  position: absolute;
  top: 0;
  left: 20%;
  right: 20%;
  height: 2px;
  background: var(--color-gold);
  border-radius: 0 0 2px 2px;
}
.tab-icon-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tab-badge {
  position: absolute;
  top: -5px;
  right: -8px;
  background: var(--color-gold);
  color: #fff;
  border-radius: 10px;
  font-size: 0.6rem;
  font-weight: 700;
  padding: 1px 4px;
  min-width: 16px;
  text-align: center;
  line-height: 1.4;
}
.tab-label {
  line-height: 1;
}
</style>
