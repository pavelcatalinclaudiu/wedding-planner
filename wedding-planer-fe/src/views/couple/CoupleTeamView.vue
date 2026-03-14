<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import type { Component } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useLeadsStore } from "@/stores/leads.store";
import { useCoupleStore } from "@/stores/couple.store";
import { bookingsApi } from "@/api/bookings.api";
import type { Booking } from "@/types/lead.types";
import ReviewModal from "@/components/couple/ReviewModal.vue";
import {
  Camera,
  Video,
  Landmark,
  Flower2,
  Utensils,
  CakeSlice,
  Music,
  Headphones,
  Star,
  CheckCircle2,
} from "lucide-vue-next";

const { t } = useI18n();

const router = useRouter();
const leadsStore = useLeadsStore();
const coupleStore = useCoupleStore();

// ── Bookings ──────────────────────────────────────────────────────────────
const bookings = ref<Booking[]>([]);

onMounted(async () => {
  if (leadsStore.leads.length === 0) await leadsStore.fetchCoupleLeads();
  try {
    const res = await bookingsApi.list();
    bookings.value = res.data;
  } catch {
    bookings.value = [];
  }
});

function bookingForLead(leadId: string): Booking | undefined {
  return bookings.value.find((b) => b.leadId === leadId);
}

function canReview(leadId: string): boolean {
  const b = bookingForLead(leadId);
  if (!b || b.hasReview) return false;
  const weddingDate = coupleStore.profile?.weddingDate;
  if (!weddingDate) return false;
  return new Date(weddingDate) < new Date();
}

function isReviewed(leadId: string): boolean {
  return bookingForLead(leadId)?.hasReview === true;
}

// ── Review modal state ─────────────────────────────────────────────────────
const reviewModal = ref<{ bookingId: string; vendorName: string } | null>(null);

function openReview(leadId: string, vendorName: string) {
  const b = bookingForLead(leadId);
  if (!b) return;
  reviewModal.value = { bookingId: b.id, vendorName };
}

function onReviewSubmitted() {
  // Optimistically mark as reviewed in local bookings list
  if (reviewModal.value) {
    const b = bookings.value.find((x) => x.id === reviewModal.value!.bookingId);
    if (b) b.hasReview = true;
  }
  reviewModal.value = null;
}

// ── Categories / slots ────────────────────────────────────────────────────
const confirmedVendors = computed(() =>
  leadsStore.leads.filter((l) => l.status === "BOOKED"),
);

const KEY_CATEGORIES: { key: string; label: string; icon: Component }[] = [
  { key: "PHOTOGRAPHER", label: "Photographer", icon: Camera },
  { key: "VIDEOGRAPHER", label: "Videographer", icon: Video },
  { key: "VENUE", label: "Venue", icon: Landmark },
  { key: "FLORIST", label: "Florist", icon: Flower2 },
  { key: "CATERER", label: "Caterer", icon: Utensils },
  { key: "CAKE", label: "Cake & Pastry", icon: CakeSlice },
  { key: "BAND", label: "Live Music", icon: Music },
  { key: "DJ", label: "DJ", icon: Headphones },
];

const filledCategories = computed(
  () => new Set(confirmedVendors.value.map((l) => l.vendorCategory)),
);

const emptySlots = computed(() =>
  KEY_CATEGORIES.filter((c) => !filledCategories.value.has(c.key)),
);
</script>

<template>
  <div class="team-view">
    <div class="page-header">
      <h2>{{ t("team.title") }}</h2>
      <p class="subtitle">{{ t("team.subtitle") }}</p>
    </div>

    <!-- Confirmed vendors -->
    <div v-if="confirmedVendors.length" class="vendor-grid">
      <div
        v-for="lead in confirmedVendors"
        :key="lead.id"
        class="vendor-card confirmed"
      >
        <div class="vendor-avatar">
          {{ lead.vendorName?.[0]?.toUpperCase() ?? "V" }}
        </div>
        <div class="vendor-info">
          <p class="vendor-name">{{ lead.vendorName }}</p>
          <p class="vendor-category">
            {{ t(`categories.${lead.vendorCategory}`) }}
          </p>
        </div>

        <!-- Review status / action -->
        <div v-if="isReviewed(lead.id)" class="reviewed-badge">
          <CheckCircle2 :size="14" />
          {{ t("review.reviewed") }}
        </div>
        <button
          v-else-if="canReview(lead.id)"
          class="review-btn"
          @click="openReview(lead.id, lead.vendorName)"
        >
          <Star :size="14" />
          {{ t("review.leaveReview") }}
        </button>

        <RouterLink :to="`/vendors/${lead.vendorId}`" class="view-link">
          {{ t("team.viewProfile") }} →
        </RouterLink>
      </div>
    </div>

    <!-- Empty category slots -->
    <div v-if="emptySlots.length" class="empty-section">
      <p class="empty-section-title">
        {{
          confirmedVendors.length
            ? t("team.stillLooking")
            : t("team.startBuilding")
        }}
      </p>
      <div class="vendor-grid">
        <div
          v-for="slot in emptySlots"
          :key="slot.key"
          class="vendor-card empty-slot"
        >
          <div class="slot-icon"><component :is="slot.icon" :size="22" /></div>
          <div class="vendor-info">
            <p class="vendor-name">{{ t(`categories.${slot.key}`) }}</p>
            <p class="vendor-category muted">{{ t("team.notYetBooked") }}</p>
          </div>
          <button
            class="find-btn"
            @click="router.push(`/vendors?category=${slot.key}`)"
          >
            {{ t("team.findVendor", { label: t(`categories.${slot.key}`) }) }}
          </button>
        </div>
      </div>
    </div>

    <!-- Review Modal -->
    <ReviewModal
      v-if="reviewModal"
      :booking-id="reviewModal.bookingId"
      :vendor-name="reviewModal.vendorName"
      @submitted="onReviewSubmitted"
      @close="reviewModal = null"
    />
  </div>
</template>

<style scoped>
h2 {
  margin: 0 0 6px;
  font-size: 1.4rem;
}
.subtitle {
  color: var(--color-muted);
  margin: 0 0 28px;
  font-size: 0.9rem;
}
.vendor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.vendor-card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
}
.vendor-card.confirmed {
  border-color: var(--color-gold);
}
.vendor-card.empty-slot {
  border-style: dashed;
  opacity: 0.85;
}
.vendor-avatar {
  width: 52px;
  height: 52px;
  background: var(--color-gold);
  color: #fff;
  border-radius: 50%;
  font-size: 1.3rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}
.slot-icon {
  width: 52px;
  height: 52px;
  background: var(--color-surface);
  border-radius: 50%;
  color: var(--color-gold);
  display: flex;
  align-items: center;
  justify-content: center;
}
.vendor-info {
  width: 100%;
}
.vendor-name {
  font-weight: 600;
  font-size: 0.95rem;
  margin: 0 0 2px;
}
.vendor-category {
  font-size: 0.8rem;
  color: var(--color-muted);
  text-transform: capitalize;
  margin: 0;
}
.muted {
  color: var(--color-muted);
}
.view-link {
  font-size: 0.85rem;
  color: var(--color-gold);
  text-decoration: none;
  font-weight: 600;
}
.view-link:hover {
  text-decoration: underline;
}
.find-btn {
  width: 100%;
  padding: 8px 14px;
  background: none;
  border: 1.5px solid var(--color-gold);
  border-radius: 8px;
  color: var(--color-gold);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}
.find-btn:hover {
  background: var(--color-gold);
  color: #fff;
}
.empty-section {
  margin-top: 32px;
}
.empty-section-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin: 0 0 16px;
}

/* ── Review ── */
.review-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--color-gold);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: filter 0.15s;
}

.review-btn:hover {
  filter: brightness(1.08);
}

.reviewed-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-green);
  padding: 6px 12px;
  background: var(--color-green-light);
  border-radius: 8px;
  width: 100%;
}
</style>
