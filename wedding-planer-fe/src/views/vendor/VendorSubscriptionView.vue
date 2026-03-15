<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { subscriptionsApi } from "@/api/subscriptions.api";
import type { Subscription } from "@/api/subscriptions.api";
import { Check } from "lucide-vue-next";

const subscription = ref<Subscription | null>(null);
const loading = ref(false);
const checkingOutPlan = ref<string | null>(null);
const upgradingPlan = ref<string | null>(null);
const cancelConfirm = ref(false);
const { t, tm } = useI18n();

const currentPlan = computed(() => subscription.value?.plan ?? "FREE");
const hasStripeSub = computed(() => !!subscription.value?.stripeSubscriptionId);

onMounted(async () => {
  loading.value = true;
  try {
    const params = new URLSearchParams(window.location.search);
    const sessionId = params.get("session_id");
    if (sessionId) {
      // Stripe redirected back after payment — sync subscription from Stripe directly
      try {
        const syncResponse = await subscriptionsApi.syncSession(sessionId);
        subscription.value = syncResponse.data;
      } catch {
        // Sync failed; fall through to normal fetch
        const response = await subscriptionsApi.get();
        subscription.value = response.data;
      }
      // Clean up URL
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

async function checkout(plan: string) {
  if (checkingOutPlan.value) return;
  checkingOutPlan.value = plan;
  checkoutError.value = "";
  try {
    const successUrl = `${window.location.origin}/vendor/subscription?session_id={CHECKOUT_SESSION_ID}`;
    const cancelUrl = `${window.location.origin}/vendor/subscription`;
    const response = await subscriptionsApi.checkout(
      plan,
      "monthly",
      successUrl,
      cancelUrl,
    );
    window.location.href = response.data.url;
  } catch (e: any) {
    checkoutError.value =
      e?.response?.data?.error ?? "Failed to start checkout. Please try again.";
  } finally {
    checkingOutPlan.value = null;
  }
}

async function upgrade(plan: string) {
  if (upgradingPlan.value) return;
  upgradingPlan.value = plan;
  checkoutError.value = "";
  try {
    const response = await subscriptionsApi.upgrade(plan);
    subscription.value = response.data;
  } catch (e: any) {
    checkoutError.value =
      e?.response?.data?.error ?? "Failed to switch plan. Please try again.";
  } finally {
    upgradingPlan.value = null;
  }
}

async function openPortal() {
  try {
    const returnUrl = `${window.location.origin}/vendor/subscription`;
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
    // Subscription is now cancelled — reload to show plans grid
    window.location.reload();
  } catch (e: any) {
    checkoutError.value =
      e?.response?.data?.error ?? "Failed to cancel subscription.";
  }
}
</script>

<template>
  <div class="subscription-view">
    <div class="page-header">
      <h2>{{ t("vendor.subscription.title") }}</h2>
      <p class="page-sub">{{ t("vendor.subscription.subtitle") }}</p>
    </div>

    <div v-if="loading" class="loading">{{ t("common.loading") }}</div>

    <div v-if="checkoutError" class="error-banner">{{ checkoutError }}</div>

    <template v-if="!loading">
      <div class="plans-grid">
        <!-- FREE -->
        <div
          class="plan-card"
          :class="{ 'is-current': currentPlan === 'FREE' }"
        >
          <span v-if="currentPlan === 'FREE'" class="current-badge">
            {{ t("vendor.subscription.currentPlan") }}
          </span>
          <h3>{{ t("vendor.subscription.freeLabel") }}</h3>
          <ul class="feature-list">
            <li
              v-for="f in tm('vendor.subscription.freeFeatures') as string[]"
              :key="f"
            >
              <Check :size="14" /> {{ f }}
            </li>
          </ul>
          <p v-if="currentPlan !== 'FREE'" class="downgrade-note">
            {{ t("vendor.subscription.cancelToDowngrade") }}
          </p>
        </div>

        <!-- STANDARD -->
        <div
          class="plan-card"
          :class="{
            featured: currentPlan !== 'STANDARD',
            'is-current': currentPlan === 'STANDARD',
          }"
        >
          <span v-if="currentPlan !== 'STANDARD'" class="featured-badge">
            {{ t("vendor.subscription.mostPopular") }}
          </span>
          <span v-else class="current-badge">
            {{ t("vendor.subscription.currentPlan") }}
          </span>
          <h3>{{ t("vendor.subscription.standardLabel") }}</h3>
          <div class="plan-price">
            <span class="price-amount">{{
              t("vendor.subscription.standardPrice")
            }}</span>
            <span class="price-period"
              >/{{ t("vendor.subscription.perMonth") }}</span
            >
          </div>
          <ul class="feature-list">
            <li
              v-for="f in tm(
                'vendor.subscription.standardFeatures',
              ) as string[]"
              :key="f"
            >
              <Check :size="14" /> {{ f }}
            </li>
          </ul>
          <template v-if="currentPlan === 'STANDARD'">
            <p
              v-if="subscription?.currentPeriodEnd && hasStripeSub"
              class="plan-renews"
            >
              {{ t("vendor.subscription.renews") }}
              {{ new Date(subscription.currentPeriodEnd).toLocaleDateString() }}
            </p>
            <p v-else-if="!hasStripeSub" class="admin-note">
              {{ t("vendor.subscription.adminAssigned") }}
            </p>
            <button v-if="hasStripeSub" class="portal-btn" @click="openPortal">
              {{ t("vendor.subscription.manageBilling") }}
            </button>
            <p v-if="subscription?.cancelAtPeriodEnd" class="cancel-notice">
              {{ t("vendor.subscription.cancelNotice") }}
            </p>
            <button
              v-if="
                hasStripeSub &&
                !cancelConfirm &&
                !subscription?.cancelAtPeriodEnd
              "
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
          </template>
          <button
            v-else
            class="upgrade-btn"
            :disabled="checkingOutPlan !== null || upgradingPlan !== null"
            :class="{
              loading:
                checkingOutPlan === 'STANDARD' || upgradingPlan === 'STANDARD',
            }"
            @click.stop="
              hasStripeSub ? upgrade('STANDARD') : checkout('STANDARD')
            "
          >
            {{
              upgradingPlan === "STANDARD"
                ? t("vendor.subscription.switching")
                : currentPlan === "FREE"
                  ? t("vendor.subscription.upgradeTo", {
                      plan: t("vendor.subscription.standardLabel"),
                    })
                  : t("vendor.subscription.switchTo", {
                      plan: t("vendor.subscription.standardLabel"),
                    })
            }}
          </button>
        </div>

        <!-- PREMIUM -->
        <div
          class="plan-card"
          :class="{ 'is-current': currentPlan === 'PREMIUM' }"
        >
          <span v-if="currentPlan === 'PREMIUM'" class="current-badge">
            {{ t("vendor.subscription.currentPlan") }}
          </span>
          <h3>{{ t("vendor.subscription.premiumLabel") }}</h3>
          <div class="plan-price">
            <span class="price-amount">{{
              t("vendor.subscription.premiumPrice")
            }}</span>
            <span class="price-period"
              >/{{ t("vendor.subscription.perMonth") }}</span
            >
          </div>
          <ul class="feature-list">
            <li
              v-for="f in tm('vendor.subscription.premiumFeatures') as string[]"
              :key="f"
            >
              <Check :size="14" /> {{ f }}
            </li>
          </ul>
          <template v-if="currentPlan === 'PREMIUM'">
            <p
              v-if="subscription?.currentPeriodEnd && hasStripeSub"
              class="plan-renews"
            >
              {{ t("vendor.subscription.renews") }}
              {{ new Date(subscription.currentPeriodEnd).toLocaleDateString() }}
            </p>
            <p v-else-if="!hasStripeSub" class="admin-note">
              {{ t("vendor.subscription.adminAssigned") }}
            </p>
            <button v-if="hasStripeSub" class="portal-btn" @click="openPortal">
              {{ t("vendor.subscription.manageBilling") }}
            </button>
            <p v-if="subscription?.cancelAtPeriodEnd" class="cancel-notice">
              {{ t("vendor.subscription.cancelNotice") }}
            </p>
            <button
              v-if="
                hasStripeSub &&
                !cancelConfirm &&
                !subscription?.cancelAtPeriodEnd
              "
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
          </template>
          <button
            v-else
            class="upgrade-btn"
            :disabled="checkingOutPlan !== null || upgradingPlan !== null"
            :class="{
              loading:
                checkingOutPlan === 'PREMIUM' || upgradingPlan === 'PREMIUM',
            }"
            @click.stop="
              hasStripeSub ? upgrade('PREMIUM') : checkout('PREMIUM')
            "
          >
            {{
              upgradingPlan === "PREMIUM"
                ? t("vendor.subscription.switching")
                : currentPlan === "FREE"
                  ? t("vendor.subscription.upgradeTo", {
                      plan: t("vendor.subscription.premiumLabel"),
                    })
                  : t("vendor.subscription.switchTo", {
                      plan: t("vendor.subscription.premiumLabel"),
                    })
            }}
          </button>
        </div>
      </div>
    </template>
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
.current-features {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 0.9rem;
}
.current-features li {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text);
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
.cancel-notice {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-amber);
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
.upgrade-btn.loading {
  opacity: 0.8;
  cursor: wait;
}
.plan-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.price-amount {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text);
}
.price-period {
  font-size: 0.82rem;
  color: var(--color-muted);
}
.current-free-card {
  border-color: var(--color-border-strong);
  background: var(--color-surface-alt);
}
.current-free-badge {
  display: inline-flex;
  align-self: flex-start;
  background: var(--color-muted, #888);
  color: #fff;
  border-radius: 20px;
  padding: 3px 14px;
  font-size: 0.75rem;
  font-weight: 700;
  white-space: nowrap;
}
.is-current {
  border-color: var(--color-gold);
  box-shadow: 0 0 0 2px rgba(212, 175, 55, 0.2);
}
.current-badge {
  display: inline-flex;
  align-self: flex-start;
  background: var(--color-gold);
  color: #fff;
  border-radius: 20px;
  padding: 3px 14px;
  font-size: 0.75rem;
  font-weight: 700;
  white-space: nowrap;
}
.admin-note {
  font-size: 0.8rem;
  color: var(--color-muted);
  margin: 0;
  font-style: italic;
}
.downgrade-note {
  font-size: 0.8rem;
  color: var(--color-muted);
  margin: 0;
}
</style>
