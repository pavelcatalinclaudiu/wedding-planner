<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { Lock } from "lucide-vue-next";
import { useFeatureAccess } from "@/composables/useFeatureAccess";

const props = defineProps<{ feature: string }>();
const { t } = useI18n();
const router = useRouter();
const { canAccess, upgradePath } = useFeatureAccess();
const accessible = computed(() => canAccess(props.feature));
</script>

<template>
  <slot v-if="accessible" />
  <div v-else class="upgrade-gate">
    <div class="gate-content">
      <Lock :size="40" class="gate-icon" />
      <h3 class="gate-title">{{ t("upgrade.title") }}</h3>
      <p class="gate-desc">
        {{
          t("upgrade.description", {
            feature: t(`upgrade.features.${feature}`),
          })
        }}
      </p>
      <button class="gate-btn" @click="router.push(upgradePath)">
        {{ t("upgrade.cta") }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.upgrade-gate {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  background: var(--bg-secondary, #f8f8f8);
  border-radius: 12px;
  margin: 16px 0;
}

.gate-content {
  text-align: center;
  max-width: 360px;
  padding: 40px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.gate-icon {
  color: var(--text-muted, #aaa);
}

.gate-title {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0;
}

.gate-desc {
  color: var(--text-muted, #888);
  font-size: 0.9rem;
  margin: 0;
}

.gate-btn {
  padding: 10px 24px;
  background: var(--primary, #6c63ff);
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.gate-btn:hover {
  opacity: 0.9;
}
</style>
