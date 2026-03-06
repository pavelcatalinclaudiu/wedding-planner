<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useLeadsStore } from "@/stores/leads.store";

const { t } = useI18n();

const router = useRouter();
const leadsStore = useLeadsStore();
const confirmedVendors = computed(() =>
  leadsStore.leads.filter((l) => l.status === "BOOKED"),
);

// Key categories shown as empty slots when not yet filled
const KEY_CATEGORIES = [
  { key: "PHOTOGRAPHER", label: "Photographer", icon: "📷" },
  { key: "VIDEOGRAPHER", label: "Videographer", icon: "🎥" },
  { key: "VENUE", label: "Venue", icon: "🏛️" },
  { key: "FLORIST", label: "Florist", icon: "💐" },
  { key: "CATERER", label: "Caterer", icon: "🍽️" },
  { key: "CAKE", label: "Cake & Pastry", icon: "🎂" },
  { key: "BAND", label: "Live Music", icon: "🎸" },
  { key: "DJ", label: "DJ", icon: "🎧" },
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
    <h2>{{ t("team.title") }}</h2>
    <p class="subtitle">{{ t("team.subtitle") }}</p>

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
            {{ lead.vendorCategory?.replace(/_/g, " ") }}
          </p>
        </div>
        <RouterLink :to="`/couple/enquiries`" class="view-link"
          >{{ t("team.viewProfile") }} →</RouterLink
        >
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
          <div class="slot-icon">{{ slot.icon }}</div>
          <div class="vendor-info">
            <p class="vendor-name">{{ slot.label }}</p>
            <p class="vendor-category muted">{{ t("team.notYetBooked") }}</p>
          </div>
          <button
            class="find-btn"
            @click="router.push(`/vendors?category=${slot.key}`)"
          >
            {{ t("team.findVendor", { label: slot.label }) }}
          </button>
        </div>
      </div>
    </div>
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
  font-size: 1.4rem;
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
</style>
