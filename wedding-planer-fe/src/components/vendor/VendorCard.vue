<script setup lang="ts">
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth.store";
import { useLeadsStore } from "@/stores/leads.store";
import type { VendorProfile } from "@/types/vendor.types";
import EnquiryModal from "./EnquiryModal.vue";
import { Camera, Star as StarIcon, MapPin } from "lucide-vue-next";

const props = defineProps<{ vendor: VendorProfile }>();
defineOptions({ inheritAttrs: false });

const router = useRouter();
const authStore = useAuthStore();
const leadsStore = useLeadsStore();
const showModal = ref(false);
const hovered = ref(false);

const coverPhoto = computed(
  () => props.vendor.coverPhoto ?? props.vendor.photos?.[0]?.url ?? null,
);

const isFeatured = computed(
  () =>
    props.vendor.tier === "PREMIUM" ||
    (props.vendor.tier as string) === "STUDIO",
);

const TERMINAL_STATUSES = ["CANCELLED", "DECLINED"] as const;

const existingLead = computed(() =>
  leadsStore.leads.find(
    (l) =>
      l.vendorId === props.vendor.id &&
      !TERMINAL_STATUSES.includes(
        l.status as (typeof TERMINAL_STATUSES)[number],
      ),
  ),
);

const canContactAgain = computed(
  () =>
    !existingLead.value &&
    leadsStore.leads.some(
      (l) =>
        l.vendorId === props.vendor.id &&
        TERMINAL_STATUSES.includes(
          l.status as (typeof TERMINAL_STATUSES)[number],
        ),
    ),
);

const categoryLabel = computed(
  () => props.vendor.category?.replace(/_/g, " ") ?? "",
);

function handleEnquire(e: Event) {
  e.preventDefault();
  e.stopPropagation();

  if (!authStore.isAuthenticated) {
    sessionStorage.setItem("pendingEnquiryVendorId", props.vendor.id);
    router.push("/register/couple");
    return;
  }

  if (authStore.user?.role === "VENDOR") return;

  if (existingLead.value) {
    router.push("/couple/enquiries");
    return;
  }

  // canContactAgain — fall through to open modal
  showModal.value = true;
}
</script>

<template>
  <RouterLink
    v-bind="$attrs"
    :to="`/vendors/${vendor.id}`"
    class="vendor-card"
    :class="{ hovered }"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
  >
    <!-- Photo area -->
    <div class="card-photo">
      <img
        v-if="coverPhoto"
        :src="coverPhoto"
        :alt="vendor.businessName"
        class="photo-img"
      />
      <div v-else class="photo-placeholder">
        {{ vendor.businessName?.[0] }}
      </div>

      <!-- Top-left badge -->
      <span v-if="isFeatured" class="badge badge-featured"
        ><StarIcon :size="11" /> Featured</span
      >

      <!-- Category label in card -->
      <span class="badge badge-category">{{ categoryLabel }}</span>

      <!-- Hover quick stats -->
      <div class="quick-stats">
        <span v-if="vendor.photos?.length"
          ><Camera :size="13" /> {{ vendor.photos.length }}</span
        >
        <span v-if="vendor.averageRating"
          ><StarIcon :size="13" /> {{ vendor.averageRating.toFixed(1) }}</span
        >
      </div>
    </div>

    <!-- Info area -->
    <div class="card-body">
      <div class="card-row-1">
        <p class="card-name">{{ vendor.businessName }}</p>
      </div>
      <p class="card-city"><MapPin :size="13" /> {{ vendor.city }}</p>

      <!-- Rating -->
      <div v-if="vendor.reviewCount" class="card-rating">
        <span class="stars">
          <span
            v-for="i in 5"
            :key="i"
            :class="[
              'star',
              i <= Math.round(vendor.averageRating ?? 0) ? 'filled' : '',
            ]"
            >★</span
          >
        </span>
        <span class="rating-num">{{ vendor.averageRating?.toFixed(1) }}</span>
        <span class="rating-count">({{ vendor.reviewCount }})</span>
      </div>

      <p class="card-price">
        <span v-if="vendor.basePrice">
          From {{ vendor.basePrice.toLocaleString() }} RON
        </span>
        <span v-else class="price-request">Price on request</span>
      </p>

      <!-- Action row -->
      <div class="card-actions" @click.prevent>
        <RouterLink :to="`/vendors/${vendor.id}`" class="view-link">
          View Profile
        </RouterLink>
        <button
          v-if="authStore.user?.role !== 'VENDOR'"
          class="enquire-btn"
          :class="{ 'already-contacted': !!existingLead }"
          @click="handleEnquire"
        >
          {{
            existingLead
              ? "In Enquiries"
              : canContactAgain
                ? "Send New Enquiry"
                : "Send Enquiry"
          }}
        </button>
      </div>
    </div>
  </RouterLink>

  <!-- Enquiry modal -->
  <Teleport to="body">
    <EnquiryModal
      v-if="showModal"
      :vendor="vendor"
      @close="showModal = false"
    />
  </Teleport>
</template>

<style scoped>
.vendor-card {
  display: flex;
  flex-direction: column;
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.vendor-card:hover,
.vendor-card.hovered {
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

/* Photo */
.card-photo {
  position: relative;
  aspect-ratio: 4 / 3;
  background: var(--color-surface);
  overflow: hidden;
}
.photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}
.vendor-card:hover .photo-img {
  transform: scale(1.04);
}
.photo-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3.5rem;
  font-weight: 900;
  color: var(--color-gold);
  background: var(--color-gold-light);
}

/* Badges */
.badge {
  position: absolute;
  border-radius: 6px;
  padding: 3px 9px;
  font-size: 0.72rem;
  font-weight: 700;
  line-height: 1.4;
}
.badge-featured {
  top: 10px;
  left: 10px;
  background: var(--color-gold);
  color: var(--color-white);
}
.badge-category {
  bottom: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--color-text);
  text-transform: capitalize;
}

/* Quick stats on hover */
.quick-stats {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.2s;
}
.vendor-card:hover .quick-stats {
  opacity: 1;
}
.quick-stats span {
  background: rgba(0, 0, 0, 0.6);
  color: var(--color-white);
  border-radius: 4px;
  padding: 2px 7px;
  font-size: 0.72rem;
  font-weight: 600;
}

/* Body */
.card-body {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}
.card-name {
  margin: 0;
  font-size: 0.97rem;
  font-weight: 700;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-city {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-muted);
}
.card-rating {
  display: flex;
  align-items: center;
  gap: 4px;
}
.stars {
  display: flex;
  gap: 1px;
}
.star {
  color: var(--color-border);
  font-size: 0.78rem;
}
.star.filled {
  color: var(--color-gold);
}
.rating-num {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text);
}
.rating-count {
  font-size: 0.75rem;
  color: var(--color-muted);
}
.card-price {
  margin: 0;
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-gold);
}
.price-request {
  color: var(--color-muted);
  font-weight: 400;
}

/* Actions */
.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--color-border);
}
.view-link {
  font-size: 0.8rem;
  color: var(--color-muted);
  text-decoration: none;
  font-weight: 500;
}
.view-link:hover {
  color: var(--color-gold);
}
.enquire-btn {
  background: var(--color-gold);
  color: var(--color-white);
  border: none;
  border-radius: 7px;
  padding: 6px 14px;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s;
}
.enquire-btn:hover {
  background: var(--color-gold-dark);
}
.enquire-btn.already-contacted {
  background: var(--color-green-light);
  color: var(--color-green);
}
</style>
