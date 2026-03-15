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
const billingType = ref<"monthly" | "onetime">("monthly");
const { t, tm } = useI18n();

onMounted(async () => {
  loading.value = true;
  try {
    const params = new URLSearchParams(window.location.search);
    const sessionId = params.get("session_id");
    if (sessionId) {
      try {
        const syncResponse = await subscriptionsApi.syncSession(sessionId);
        subscription.value = syncResponse.data;
      } catch {
        const response = await subscriptionsApi.get();
        subscription.value = response.data;
      }
      window.history.replaceState({}, "", window.location.pathname);
    } else {
      const response = await subscriptionsApi.get();
      subscription.value = response.data;
      // If no paid plan found locally, check Stripe directly to recover from
      // missed webhooks or redirects (e.g. user navigated away during payment)
      if (!subscription.value || subscription.value.plan === "FREE") {
        try {
          const syncResponse = await subscriptionsApi.sync();
          if (syncResponse.data) subscription.value = syncResponse.data;
        } catch {
          // Best-effort — ignore errors
        }
      }
    }
  } catch {
    subscription.value = null;
  } finally {
    loading.value = false;
  }
});

const checkoutError = ref("");

async function checkout() {
  checkingOut.value = true;
  checkoutError.value = "";
  try {
    const successUrl = `${window.location.origin}/couple/subscription?session_id={CHECKOUT_SESSION_ID}`;
    const cancelUrl = `${window.location.origin}/couple/subscription`;
    const response = await subscriptionsApi.checkout(
      "DREAM_WEDDING",
      billingType.value,
      successUrl,
      cancelUrl,
    );
    window.location.href = response.data.url;
  } catch (e: any) {
    checkoutError.value =
      e?.response?.data?.error ?? "Failed to start checkout. Please try again.";
  } finally {
    checkingOut.value = false;
  }
}

async function openPortal() {
  try {
    const returnUrl = `${window.location.origin}/couple/subscription`;
    const response = await subscriptionsApi.portal(returnUrl);
    window.open(response.data.url, "_blank");
  } catch (e: any) {
    checkoutError.value =
      e?.response?.data?.error ?? "Failed to open billing portal.";
  }
}

async function cancel() {
  try {
    await subscriptionsApi.cancel();
    const response = await subscriptionsApi.get();
    subscription.value = response.data;
    cancelConfirm.value = false;
  } catch (e: any) {
    checkoutError.value =
      e?.response?.data?.error ?? "Failed to cancel subscription.";
  }
}
</script>

<template>
  <div class="subscription-view">
    <div class="page-header">
      <h2>{{ t("couple.subscription.title") }}</h2>
      <p class="page-sub">{{ t("couple.subscription.subtitle") }}</p>
    </div>

    <div v-if="loading" class="loading">{{ t("common.loading") }}</div>

    <div v-if="checkoutError" class="error-banner">{{ checkoutError }}</div>

    <!-- Active paid plan -->
    <div
      v-else-if="
        subscription?.status === 'active' &&
        subscription?.plan === 'DREAM_WEDDING'
      "
      class="current-plan"
    >
      <div class="plan-badge">
        {{ t("couple.subscription.dreamWeddingLabel") }}
      </div>
      <p class="plan-renews">
        {{ t("couple.subscription.renews") }}
        {{
          subscription.currentPeriodEnd
            ? new Date(subscription.currentPeriodEnd).toLocaleDateString()
            : ""
        }}
      </p>
      <button class="portal-btn" @click="openPortal">
        {{ t("couple.subscription.manageBilling") }}
      </button>
      <button
        v-if="!cancelConfirm"
        class="cancel-link"
        @click="cancelConfirm = true"
      >
        {{ t("couple.subscription.cancel") }}
      </button>
      <div v-if="cancelConfirm" class="cancel-confirm">
        <p>{{ t("couple.subscription.cancelConfirm") }}</p>
        <button class="confirm-cancel-btn" @click="cancel">
          {{ t("couple.subscription.yesCancel") }}
        </button>
        <button class="keep-btn" @click="cancelConfirm = false">
          {{ t("couple.subscription.keep") }}
        </button>
      </div>
    </div>

    <!-- Upgrade prompt (FREE plan) -->
    <div v-else class="plans-grid">
      <!-- Free plan card -->
      <div class="plan-card">
        <h3>{{ t("couple.subscription.freePlanName") }}</h3>
        <div class="plan-price">
          <span class="price-amount">{{
            t("couple.subscription.freePrice")
          }}</span>
        </div>
        <ul class="feature-list">
          <li
            v-for="f in tm('couple.subscription.freeFeatures') as string[]"
            :key="f"
          >
            <Check :size="14" /> {{ f }}
          </li>
        </ul>
        <button class="current-btn" disabled>
          {{ t("couple.subscription.currentPlan") }}
        </button>
      </div>

      <!-- Dream Wedding card -->
      <div class="plan-card featured">
        <div class="featured-badge">
          {{ t("couple.subscription.mostPopular") }}
        </div>
        <h3>{{ t("couple.subscription.dreamWeddingLabel") }}</h3>

        <!-- Billing toggle -->
        <div class="billing-toggle">
          <button
            :class="{ active: billingType === 'monthly' }"
            @click="billingType = 'monthly'"
          >
            {{ t("couple.subscription.monthly") }}
            <span class="price-tag">59 RON</span>
          </button>
          <button
            :class="{ active: billingType === 'onetime' }"
            @click="billingType = 'onetime'"
          >
            {{ t("couple.subscription.oneTime") }}
            <span class="price-tag">299 RON</span>
          </button>
        </div>

        <ul class="feature-list">
          <li
            v-for="f in tm('couple.subscription.dreamFeatures') as string[]"
            :key="f"
          >
            <Check :size="14" /> {{ f }}
          </li>
        </ul>
        <button class="upgrade-btn" :disabled="checkingOut" @click="checkout">
          {{ t("couple.subscription.upgradeBtn") }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
h2 {
  margin: 0 0 4px;
  font-size: 1.4rem;
}
.page-sub {
  margin: 0 0 24px;
  font-size: 0.88rem;
  color: var(--color-muted);
}
.loading {
  color: var(--color-muted);
  text-align: center;
  padding: 48px;
}
.error-banner {
  background: #fef2f2;
  border: 1px solid #fca5a5;
  color: #b91c1c;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 0.9rem;
}
.current-plan {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 420px;
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
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
  max-width: 760px;
}
.plan-card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 28px 24px;
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
  font-size: 1.15rem;
  font-weight: 700;
}
.plan-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.price-amount {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-gold);
}
.billing-toggle {
  display: flex;
  gap: 8px;
}
.billing-toggle button {
  flex: 1;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
  color: var(--color-text);
  padding: 7px 8px;
  cursor: pointer;
  font-size: 0.8rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  transition: border-color 0.15s;
}
.billing-toggle button.active {
  border-color: var(--color-gold);
  background: rgba(212, 175, 55, 0.08);
  font-weight: 700;
}
.price-tag {
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--color-gold);
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
  display: flex;
  align-items: center;
  gap: 6px;
}
.upgrade-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 11px;
  font-weight: 700;
  cursor: pointer;
}
.upgrade-btn:disabled {
  opacity: 0.5;
}
.current-btn {
  background: var(--color-surface);
  color: var(--color-muted);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 11px;
  font-weight: 600;
  cursor: default;
}
</style>
