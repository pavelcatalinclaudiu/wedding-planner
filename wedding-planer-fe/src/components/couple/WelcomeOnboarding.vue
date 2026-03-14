<script setup lang="ts">
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { useCoupleStore } from "@/stores/couple.store";
import { useAuthStore } from "@/stores/auth.store";
import {
  Heart,
  CalendarDays,
  Wallet,
  Search,
  ArrowRight,
  CheckCircle2,
} from "lucide-vue-next";

const router = useRouter();
const coupleStore = useCoupleStore();
const authStore = useAuthStore();
const emit = defineEmits<{ close: [] }>();

const STORAGE_KEY = `onboarding_completed_${authStore.user?.id ?? "guest"}`;

const step = ref(1);
const TOTAL_STEPS = 4;

// Step 2
const weddingDate = ref(coupleStore.profile?.weddingDate ?? "");
const weddingLocation = ref(coupleStore.profile?.weddingLocation ?? "");

// Step 3
const totalBudget = ref(
  coupleStore.profile?.totalBudget
    ? String(coupleStore.profile.totalBudget)
    : "",
);

const saving = ref(false);

const partner1 = computed(() => coupleStore.profile?.partner1Name ?? "you");
const partner2 = computed(() => coupleStore.profile?.partner2Name ?? "");

async function next() {
  if (step.value === 2 && weddingDate.value) {
    saving.value = true;
    try {
      await coupleStore.updateProfile({
        weddingDate: weddingDate.value,
        weddingLocation: weddingLocation.value || undefined,
      });
    } finally {
      saving.value = false;
    }
  }
  if (step.value === 3 && totalBudget.value) {
    saving.value = true;
    try {
      await coupleStore.updateProfile({
        totalBudget: Number(totalBudget.value),
      });
    } finally {
      saving.value = false;
    }
  }
  if (step.value < TOTAL_STEPS) {
    step.value++;
  } else {
    finish();
  }
}

function skip() {
  if (step.value < TOTAL_STEPS) {
    step.value++;
  } else {
    finish();
  }
}

function finish() {
  localStorage.setItem(STORAGE_KEY, "1");
  emit("close");
}

function goTo(path: string) {
  finish();
  router.push(path);
}
</script>

<template>
  <Teleport to="body">
    <Transition name="onboarding-fade">
      <div class="onboarding-backdrop" @click.self="skip">
        <div class="onboarding-modal">
          <!-- Progress dots -->
          <div class="progress-dots">
            <span
              v-for="i in TOTAL_STEPS"
              :key="i"
              class="dot"
              :class="{ active: i === step, done: i < step }"
            ></span>
          </div>

          <!-- STEP 1 — Welcome -->
          <template v-if="step === 1">
            <div class="step-icon step-icon-heart">
              <Heart :size="32" fill="currentColor" />
            </div>
            <h2 class="onboarding-title">
              Welcome{{ partner1 !== "you" ? `, ${partner1}` : "" }}
              <template v-if="partner2"> &amp; {{ partner2 }}</template
              >!
            </h2>
            <p class="onboarding-text">
              You're about to start planning your perfect day. Let's get a few
              things set up so we can personalise your experience.
            </p>
            <p class="onboarding-sub">This takes about 2 minutes.</p>
            <button class="btn-next" @click="next">
              Let's start <ArrowRight :size="16" />
            </button>
          </template>

          <!-- STEP 2 — Wedding date -->
          <template v-if="step === 2">
            <div class="step-icon step-icon-gold">
              <CalendarDays :size="28" />
            </div>
            <h2 class="onboarding-title">When's the big day?</h2>
            <p class="onboarding-text">
              Your wedding date helps us calculate your countdown and deadlines.
            </p>
            <div class="onboarding-form">
              <label class="form-label">Wedding date</label>
              <input
                v-model="weddingDate"
                type="date"
                class="form-input"
                :min="new Date().toISOString().slice(0, 10)"
              />
              <label class="form-label" style="margin-top: 12px"
                >Location (optional)</label
              >
              <input
                v-model="weddingLocation"
                type="text"
                class="form-input"
                placeholder="e.g. Rome, Italy"
              />
            </div>
            <div class="onboarding-footer">
              <button class="btn-skip" @click="skip">Skip for now</button>
              <button class="btn-next" :disabled="saving" @click="next">
                {{ saving ? "Saving…" : "Continue" }}
                <ArrowRight v-if="!saving" :size="16" />
              </button>
            </div>
          </template>

          <!-- STEP 3 — Budget -->
          <template v-if="step === 3">
            <div class="step-icon step-icon-green">
              <Wallet :size="28" />
            </div>
            <h2 class="onboarding-title">What's your budget?</h2>
            <p class="onboarding-text">
              Setting a total budget lets us show you how much you've spent and
              how much you have left.
            </p>
            <div class="onboarding-form">
              <label class="form-label">Total budget (€)</label>
              <input
                v-model="totalBudget"
                type="number"
                min="0"
                step="500"
                class="form-input"
                placeholder="e.g. 20000"
              />
            </div>
            <div class="onboarding-footer">
              <button class="btn-skip" @click="skip">Skip for now</button>
              <button class="btn-next" :disabled="saving" @click="next">
                {{ saving ? "Saving…" : "Continue" }}
                <ArrowRight v-if="!saving" :size="16" />
              </button>
            </div>
          </template>

          <!-- STEP 4 — Ready! -->
          <template v-if="step === 4">
            <div class="step-icon step-icon-done">
              <CheckCircle2 :size="32" />
            </div>
            <h2 class="onboarding-title">You're all set!</h2>
            <p class="onboarding-text">
              Your dashboard is ready. Here's where to start:
            </p>
            <div class="quick-actions">
              <button class="quick-action" @click="goTo('/vendors')">
                <div class="qa-icon"><Search :size="20" /></div>
                <div class="qa-body">
                  <span class="qa-title">Find vendors</span>
                  <span class="qa-sub"
                    >Browse photographers, venues &amp; more</span
                  >
                </div>
                <ArrowRight :size="16" class="qa-arrow" />
              </button>
              <button class="quick-action" @click="goTo('/couple/guests')">
                <div class="qa-icon"><Heart :size="20" /></div>
                <div class="qa-body">
                  <span class="qa-title">Build your guest list</span>
                  <span class="qa-sub">Add guests and track RSVPs</span>
                </div>
                <ArrowRight :size="16" class="qa-arrow" />
              </button>
              <button class="quick-action" @click="goTo('/couple/checklist')">
                <div class="qa-icon"><Wallet :size="20" /></div>
                <div class="qa-body">
                  <span class="qa-title">View your checklist</span>
                  <span class="qa-sub">We've pre-loaded tasks for you</span>
                </div>
                <ArrowRight :size="16" class="qa-arrow" />
              </button>
            </div>
            <button class="btn-next" style="margin-top: 8px" @click="finish">
              Go to dashboard
            </button>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.onboarding-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 8000;
  padding: 16px;
}

.onboarding-modal {
  background: var(--color-white);
  border-radius: 20px;
  padding: 40px 40px 32px;
  width: 100%;
  max-width: 480px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.22);
  position: relative;
}

@media (max-width: 540px) {
  .onboarding-modal {
    padding: 28px 20px 24px;
  }
}

/* Progress dots */
.progress-dots {
  display: flex;
  gap: 6px;
  margin-bottom: 28px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-border);
  transition:
    background 0.25s,
    transform 0.25s;
}
.dot.active {
  background: var(--color-gold);
  transform: scale(1.3);
}
.dot.done {
  background: var(--color-green);
}

/* Step icon */
.step-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}
.step-icon-heart {
  background: #ffe5ec;
  color: #e05580;
}
.step-icon-gold {
  background: var(--color-gold-light);
  color: var(--color-gold);
}
.step-icon-green {
  background: var(--color-green-light);
  color: var(--color-green);
}
.step-icon-done {
  background: var(--color-green-light);
  color: var(--color-green);
}

/* Text */
.onboarding-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 12px;
}
.onboarding-text {
  font-size: 0.95rem;
  color: var(--color-muted);
  max-width: 340px;
  line-height: 1.6;
  margin: 0 0 6px;
}
.onboarding-sub {
  font-size: 0.82rem;
  color: var(--color-muted);
  margin: 0 0 28px;
  opacity: 0.7;
}

/* Form */
.onboarding-form {
  width: 100%;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 16px;
}
.form-label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-muted);
}
.form-input {
  background: var(--color-white);
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 0.95rem;
  font-family: inherit;
  color: var(--color-text);
  width: 100%;
  outline: none;
  transition: border-color 0.15s;
}
.form-input:focus {
  border-color: var(--color-gold);
}

/* Footer / buttons */
.onboarding-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-top: 24px;
  gap: 12px;
}
.btn-next {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 12px 24px;
  font-size: 0.95rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition:
    background 0.15s,
    transform 0.1s,
    opacity 0.15s;
  margin-top: 24px;
}
.btn-next:hover:not(:disabled) {
  background: var(--color-gold-dark);
}
.btn-next:active:not(:disabled) {
  transform: scale(0.97);
}
.btn-next:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.onboarding-footer .btn-next {
  margin-top: 0;
}
.btn-skip {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--color-muted);
  font-family: inherit;
  transition: color 0.15s;
}
.btn-skip:hover {
  color: var(--color-text);
}

/* Quick actions (step 4) */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  margin-top: 16px;
}
.quick-action {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
  width: 100%;
}
.quick-action:hover {
  border-color: var(--color-gold);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.07);
}
.qa-icon {
  width: 38px;
  height: 38px;
  background: var(--color-gold-light);
  color: var(--color-gold);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.qa-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.qa-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}
.qa-sub {
  font-size: 0.78rem;
  color: var(--color-muted);
}
.qa-arrow {
  color: var(--color-muted);
  flex-shrink: 0;
}

/* Transition */
.onboarding-fade-enter-active,
.onboarding-fade-leave-active {
  transition: opacity 0.22s ease;
}
.onboarding-fade-enter-from,
.onboarding-fade-leave-to {
  opacity: 0;
}
.onboarding-fade-enter-active .onboarding-modal,
.onboarding-fade-leave-active .onboarding-modal {
  transition: transform 0.22s ease;
}
.onboarding-fade-enter-from .onboarding-modal {
  transform: translateY(16px) scale(0.97);
}
.onboarding-fade-leave-to .onboarding-modal {
  transform: translateY(8px) scale(0.98);
}
</style>
