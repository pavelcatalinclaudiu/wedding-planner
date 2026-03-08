<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { subscriptionsApi } from "@/api/subscriptions.api";
import type { Subscription } from "@/api/subscriptions.api";
import { Check } from "lucide-vue-next";

const subscription = ref<Subscription | null>(null);
const loading = ref(false);
const checkingOut = ref(false);
const cancelConfirm = ref(false);
const { t, tm } = useI18n();

onMounted(async () => {
  loading.value = true;
  try {
    subscription.value = await subscriptionsApi.get();
  } catch {
    subscription.value = null;
  } finally {
    loading.value = false;
  }
});

async function checkout(plan: string) {
  checkingOut.value = true;
  try {
    const { url } = await subscriptionsApi.checkout(plan);
    window.location.href = url;
  } finally {
    checkingOut.value = false;
  }
}

async function openPortal() {
  const { url } = await subscriptionsApi.portal();
  window.open(url, "_blank");
}

async function cancel() {
  await subscriptionsApi.cancel();
  subscription.value = await subscriptionsApi.get();
  cancelConfirm.value = false;
}
</script>

<template>
  <div class="subscription-view">
    <h2>{{ t("vendor.subscription.title") }}</h2>

    <div v-if="loading" class="loading">{{ t("common.loading") }}</div>

    <div v-else-if="subscription?.status === 'ACTIVE'" class="current-plan">
      <div class="plan-badge">
        {{ subscription.plan }} {{ t("vendor.subscription.plan") }}
      </div>
      <p class="plan-renews">
        {{ t("vendor.subscription.renews") }}
        {{ new Date(subscription.currentPeriodEnd).toLocaleDateString() }}
      </p>
      <button class="portal-btn" @click="openPortal">
        {{ t("vendor.subscription.manageBilling") }}
      </button>
      <button
        v-if="!cancelConfirm"
        class="cancel-link"
        @click="cancelConfirm = true"
      >
        {{ t("vendor.subscription.cancel") }}
      </button>
      <div v-if="cancelConfirm" class="cancel-confirm">
        <p>{{ t("vendor.subscription.cancelConfirm") }}</p>
        <button class="confirm-cancel-btn" @click="cancel">
          {{ t("vendor.subscription.yesCancel") }}
        </button>
        <button class="keep-btn" @click="cancelConfirm = false">
          {{ t("vendor.subscription.keep") }}
        </button>
      </div>
    </div>

    <div v-else class="plans-grid">
      <div
        v-for="plan in ['BASIC', 'PRO', 'ELITE']"
        :key="plan"
        class="plan-card"
        :class="{ featured: plan === 'PRO' }"
      >
        <div v-if="plan === 'PRO'" class="featured-badge">
          {{ t("vendor.subscription.mostPopular") }}
        </div>
        <h3>{{ plan }}</h3>
        <ul class="feature-list">
          <li
            v-for="f in tm(
              'vendor.subscription.' + plan.toLowerCase() + 'Features',
            ) as string[]"
            :key="f"
          >
            <Check :size="14" /> {{ f }}
          </li>
        </ul>
        <button
          class="upgrade-btn"
          :disabled="checkingOut"
          @click="checkout(plan)"
        >
          {{
            plan === "BASIC"
              ? t("vendor.subscription.getStartedFree")
              : t("vendor.subscription.upgradeTo", { plan })
          }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
h2 {
  margin: 0 0 24px;
  font-size: 1.4rem;
}
.loading {
  color: var(--color-muted);
  text-align: center;
  padding: 48px;
}
.current-plan {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 400px;
}
.plan-badge {
  background: var(--color-gold);
  color: #fff;
  border-radius: 20px;
  padding: 4px 16px;
  font-size: 0.9rem;
  font-weight: 700;
  display: inline-flex;
  align-self: flex-start;
}
.plan-renews {
  color: var(--color-muted);
  font-size: 0.9rem;
  margin: 0;
}
.portal-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 9px 20px;
  font-weight: 700;
  cursor: pointer;
  align-self: flex-start;
}
.cancel-link {
  background: none;
  border: none;
  color: var(--color-muted);
  cursor: pointer;
  font-size: 0.85rem;
  text-decoration: underline;
  align-self: flex-start;
  padding: 0;
}
.cancel-confirm {
  background: #fff5f5;
  border: 1px solid var(--color-error);
  border-radius: 8px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.cancel-confirm p {
  margin: 0;
  font-size: 0.9rem;
}
.confirm-cancel-btn {
  background: var(--color-error);
  color: #fff;
  border: none;
  border-radius: 7px;
  padding: 7px 14px;
  font-weight: 700;
  cursor: pointer;
  align-self: flex-start;
}
.keep-btn {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 7px;
  padding: 7px 14px;
  cursor: pointer;
  align-self: flex-start;
}
.plans-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
.plan-card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
}
.plan-card.featured {
  border-color: var(--color-gold);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}
.featured-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--color-gold);
  color: #fff;
  border-radius: 20px;
  padding: 3px 14px;
  font-size: 0.75rem;
  font-weight: 700;
  white-space: nowrap;
}
.plan-card h3 {
  margin: 0;
  font-size: 1.1rem;
}
.feature-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}
.feature-list li {
  font-size: 0.85rem;
  color: var(--color-text);
}
.upgrade-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 10px;
  font-weight: 700;
  cursor: pointer;
}
.upgrade-btn:disabled {
  opacity: 0.5;
}
</style>
