<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth.store";
import { useCoupleStore } from "@/stores/couple.store";
import { leadsApi } from "@/api/leads.api";
import { vendorApi } from "@/api/vendor.api";
import type { VendorProfile } from "@/types/vendor.types";
import AvailabilityCalendar from "@/components/vendor/AvailabilityCalendar.vue";
import { Gem, X, Users, Sparkles } from "lucide-vue-next";

const props = defineProps<{
  vendor: VendorProfile;
  avgResponseHours?: number;
}>();

function formatResponseTime(hours: number | undefined | null): string {
  if (!hours || hours <= 0) return "within a few hours";
  if (hours < 1) return "under an hour";
  if (hours <= 2) return "within a couple of hours";
  if (hours <= 6) return "within a few hours";
  if (hours <= 12) return "within half a day";
  if (hours <= 24) return "within 24 hours";
  if (hours <= 48) return "within 2 days";
  return "within a few days";
}
const emit = defineEmits<{ close: []; success: [] }>();

const router = useRouter();
const authStore = useAuthStore();
const coupleStore = useCoupleStore();

const message = ref("");
const budget = ref<number | null>(null);
const selectedPackage = ref<string>("");
const preferredDate = ref("");
const blockedDates = ref<string[]>([]);
const loadingAvailability = ref(false);
const submitting = ref(false);
const error = ref("");

const MIN_CHARS = 20;
const charCount = computed(() => message.value.length);
const isValid = computed(() => charCount.value >= MIN_CHARS);

const coupleProfile = computed(() => coupleStore.profile);

const coverPhoto = computed(
  () => props.vendor.coverPhoto ?? props.vendor.photos?.[0]?.url ?? null,
);

const smartPlaceholder = computed(() => {
  if (coupleProfile.value?.partner1Name) {
    const p1 = coupleProfile.value.partner1Name;
    const p2 = coupleProfile.value.partner2Name
      ? ` & ${coupleProfile.value.partner2Name}`
      : "";
    const date = coupleProfile.value.weddingDate
      ? ` on ${new Date(coupleProfile.value.weddingDate).toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" })}`
      : "";
    const loc = coupleProfile.value.weddingLocation
      ? ` in ${coupleProfile.value.weddingLocation}`
      : "";
    return `Hi! We're ${p1}${p2} getting married${date}${loc}.\nWe're looking for a ${props.vendor.category?.replace(/_/g, " ").toLowerCase()} for our ceremony and reception.\nWould love to hear about your availability and packages.`;
  }
  return `Hi! We're getting married and looking for a ${props.vendor.category?.replace(/_/g, " ").toLowerCase()}.\nCould you share your availability and packages?`;
});

onMounted(async () => {
  if (authStore.isAuthenticated && !coupleStore.profile) {
    try {
      await coupleStore.fetchProfile();
    } catch {}
  }
  // Fetch vendor's blocked dates so couple can pick an available date
  loadingAvailability.value = true;
  try {
    const res = await vendorApi.getPublicAvailability(props.vendor.id);
    blockedDates.value = res.data.map((a) => a.date);
  } catch {
    // Non-critical — ignore if endpoint not available
  } finally {
    loadingAvailability.value = false;
  }
  // ESC to close
  window.addEventListener("keydown", onKeydown);
});
onUnmounted(() => window.removeEventListener("keydown", onKeydown));

function onKeydown(e: KeyboardEvent) {
  if (e.key === "Escape") emit("close");
}

async function submit() {
  if (!isValid.value || submitting.value) return;
  submitting.value = true;
  error.value = "";

  try {
    const parts: string[] = [];
    if (selectedPackage.value)
      parts.push(`Package interest: ${selectedPackage.value}`);
    if (preferredDate.value) {
      const fmtDate = new Date(preferredDate.value).toLocaleDateString(
        "en-GB",
        { day: "numeric", month: "long", year: "numeric" },
      );
      parts.push(`Preferred date: ${fmtDate}`);
    }
    parts.push(message.value);
    const fullMessage = parts.join("\n\n");

    await leadsApi.create({
      vendorId: props.vendor.id,
      eventDate: preferredDate.value || undefined,
      budget: budget.value ?? undefined,
      message: fullMessage,
    });

    emit("success");
    emit("close");
    router.push("/couple/enquiries");
  } catch (e: any) {
    const status = e?.response?.status;
    if (status === 409) {
      error.value = "You already have an active enquiry with this vendor.";
    } else {
      error.value =
        e?.response?.data?.message ?? "Something went wrong. Please try again.";
    }
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <!-- Backdrop -->
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal" role="dialog" aria-modal="true">
      <!-- Header -->
      <div class="modal-header">
        <div class="modal-vendor-info">
          <div class="vendor-avatar">
            <img
              v-if="coverPhoto"
              :src="coverPhoto"
              :alt="vendor.businessName"
            />
            <span v-else>{{ vendor.businessName?.[0] }}</span>
          </div>
          <div>
            <p class="vendor-name">{{ vendor.businessName }}</p>
            <p class="vendor-meta">
              {{ vendor.category?.replace(/_/g, " ") }} · {{ vendor.city }}
            </p>
          </div>
        </div>
        <button class="close-btn" @click="emit('close')">
          <X :size="18" />
        </button>
      </div>

      <div class="modal-body">
        <!-- Couple info preview -->
        <div v-if="coupleProfile" class="couple-preview">
          <div class="couple-preview-row">
            <span class="preview-icon"><Gem :size="28" /></span>
            <span v-if="coupleProfile.weddingDate">
              {{
                new Date(coupleProfile.weddingDate).toLocaleDateString(
                  "en-GB",
                  { day: "numeric", month: "long", year: "numeric" },
                )
              }}
              <span v-if="coupleProfile.weddingLocation">
                · {{ coupleProfile.weddingLocation }}</span
              >
            </span>
            <span v-else class="muted-text">No wedding date set yet</span>
          </div>
          <div
            v-if="coupleProfile.estimatedGuestCount"
            class="couple-preview-row"
          >
            <Users :size="14" />
            <span>{{ coupleProfile.estimatedGuestCount }} guests</span>
          </div>
          <p class="preview-note">
            This info is shared automatically with the vendor
          </p>
        </div>

        <hr class="divider" />

        <!-- Preferred date -->
        <div class="field-group">
          <label class="field-label">Preferred date (optional)</label>
          <AvailabilityCalendar
            v-model="preferredDate"
            :blocked-dates="blockedDates"
            :loading="loadingAvailability"
          />
          <p class="field-hint">
            Red dates are already booked. The vendor will confirm actual
            availability.
          </p>
        </div>

        <!-- Package selection -->
        <div v-if="vendor.packages?.length" class="field-group">
          <label class="field-label">Package interest (optional)</label>
          <div class="package-options">
            <label class="package-option">
              <input v-model="selectedPackage" type="radio" value="" />
              <span>No preference / custom quote</span>
            </label>
            <label
              v-for="pkg in vendor.packages"
              :key="pkg.id"
              class="package-option"
            >
              <input v-model="selectedPackage" type="radio" :value="pkg.name" />
              <span
                >{{ pkg.name }} — {{ pkg.price?.toLocaleString() }} RON</span
              >
            </label>
          </div>
        </div>

        <!-- Message composer -->
        <div class="field-group">
          <label class="field-label">
            Introduce yourself and describe what you're looking for
          </label>
          <textarea
            v-model="message"
            class="message-textarea"
            :placeholder="smartPlaceholder"
            rows="5"
            maxlength="2000"
          />
          <div
            class="char-count"
            :class="{ invalid: charCount > 0 && charCount < MIN_CHARS }"
          >
            {{ charCount }}/2000
            <span
              v-if="charCount > 0 && charCount < MIN_CHARS"
              class="char-hint"
            >
              — please write at least {{ MIN_CHARS - charCount }} more
              characters
            </span>
          </div>
        </div>

        <!-- Budget field -->
        <div class="field-group">
          <label class="field-label">Approximate budget (optional)</label>
          <div class="budget-input-wrap">
            <span class="budget-prefix">RON</span>
            <input
              v-model.number="budget"
              type="number"
              class="budget-input"
              placeholder="e.g. 5000"
              min="0"
              max="9999999999"
              step="0.01"
            />
          </div>
          <p class="field-hint">Helps the vendor send you the right quote</p>
        </div>

        <!-- Error -->
        <div v-if="error" class="error-box">{{ error }}</div>

        <!-- Submit -->
        <button
          class="submit-btn"
          :disabled="!isValid || submitting"
          @click="submit"
        >
          <span v-if="submitting">Sending…</span>
          <span v-else><Sparkles :size="14" /> Send Enquiry</span>
        </button>

        <p class="submit-note">
          {{ vendor.businessName }} will receive your wedding details and
          message. They typically reply
          {{ formatResponseTime(props.avgResponseHours) }}.
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  animation: fade-in 0.15s ease;
}
@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal {
  background: var(--color-white);
  border-radius: 18px;
  width: 100%;
  max-width: 520px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slide-up 0.2s ease;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}
@keyframes slide-up {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 24px 20px;
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  background: var(--color-white);
  z-index: 1;
}
.modal-vendor-info {
  display: flex;
  align-items: center;
  gap: 14px;
}
.vendor-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--color-gold-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 1.2rem;
  color: var(--color-gold);
  flex-shrink: 0;
}
.vendor-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.vendor-name {
  margin: 0;
  font-size: 0.97rem;
  font-weight: 700;
  color: var(--color-text);
}
.vendor-meta {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-muted);
  text-transform: capitalize;
}
.close-btn {
  background: var(--color-surface);
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 0.85rem;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: background 0.15s;
}
.close-btn:hover {
  background: var(--color-border);
}

.modal-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Couple preview */
.couple-preview {
  background: var(--color-gold-light);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.couple-preview-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.87rem;
  color: var(--color-text);
}
.preview-icon {
  font-size: 0.9rem;
}
.preview-note {
  margin: 4px 0 0;
  font-size: 0.75rem;
  color: var(--color-muted);
}
.muted-text {
  color: var(--color-muted);
}
.divider {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 0;
}

/* Fields */
.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text);
}
.field-hint {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-muted);
}

/* Package options */
.package-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.package-option {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  color: var(--color-text);
  cursor: pointer;
}

/* Textarea */
.message-textarea {
  border: 1.5px solid var(--color-border);
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 0.87rem;
  font-family: inherit;
  line-height: 1.6;
  resize: vertical;
  outline: none;
  transition: border-color 0.15s;
  color: var(--color-text);
}
.message-textarea:focus {
  border-color: var(--color-gold);
}
.char-count {
  font-size: 0.75rem;
  color: var(--color-muted);
  text-align: right;
}
.char-count.invalid {
  color: var(--color-error);
}
.char-hint {
  font-style: italic;
}

/* Budget */
.budget-input-wrap {
  display: flex;
  align-items: center;
  border: 1.5px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
  transition: border-color 0.15s;
}
.budget-input-wrap:focus-within {
  border-color: var(--color-gold);
}
.budget-prefix {
  padding: 0 12px;
  font-size: 0.85rem;
  color: var(--color-muted);
  border-right: 1px solid var(--color-border);
  height: 100%;
  display: flex;
  align-items: center;
}
.budget-input {
  border: none;
  outline: none;
  padding: 11px 14px;
  font-size: 0.87rem;
  flex: 1;
  background: transparent;
}

/* Error */
.error-box {
  background: var(--color-error-light);
  border: 1px solid var(--color-error);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 0.85rem;
  color: var(--color-error);
}

/* Submit */
.submit-btn {
  background: var(--color-gold);
  color: var(--color-white);
  border: none;
  border-radius: 10px;
  padding: 14px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  width: 100%;
  transition: background 0.15s;
}
.submit-btn:hover:not(:disabled) {
  background: var(--color-gold-dark);
}
.submit-btn:disabled {
  background: var(--color-border);
  color: var(--color-muted);
  cursor: not-allowed;
}
.submit-note {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-muted);
  text-align: center;
  line-height: 1.5;
}
</style>
