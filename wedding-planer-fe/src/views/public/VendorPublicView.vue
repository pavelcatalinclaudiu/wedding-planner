<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { vendorApi } from "@/api/vendor.api";
import { networkApi } from "@/api/network.api";
import { useAuthStore } from "@/stores/auth.store";
import { useLeadsStore } from "@/stores/leads.store";
import { reviewsApi } from "@/api/reviews.api";
import { useAuthRedirect } from "@/composables/useAuthRedirect";
import EnquiryModal from "@/components/vendor/EnquiryModal.vue";
import PublicNavbar from "@/components/public/PublicNavbar.vue";
import type { VendorProfile, VendorPartner } from "@/types/vendor.types";
import {
  X,
  MapPin,
  Calendar,
  Star as StarIcon,
  Globe,
  MessageSquare,
  Clock,
  Camera,
  Users,
} from "lucide-vue-next";

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const authStore = useAuthStore();
const leadsStore = useLeadsStore();

const vendor = ref<VendorProfile | null>(null);
const loading = ref(false);
const reviews = ref<any[]>([]);
const partners = ref<VendorPartner[]>([]);
const showModal = ref(false);
const lightboxPhoto = ref<string | null>(null);
const avgResponseHours = ref<number | null>(null);

function formatResponseTime(hours: number | null): string {
  if (hours === null || hours <= 0) return t("publicVendor.rtWithin24h");
  if (hours < 1) return t("publicVendor.rtUnder1h");
  if (hours <= 2) return t("publicVendor.rtCouple");
  if (hours <= 6) return t("publicVendor.rtFewHours");
  if (hours <= 12) return t("publicVendor.rtHalfDay");
  if (hours <= 24) return t("publicVendor.rtWithin24h");
  if (hours <= 48) return t("publicVendor.rt2Days");
  return t("publicVendor.rtFewDays");
}

const TERMINAL_STATUSES = ["CANCELLED", "DECLINED"] as const;

const existingLead = computed(() =>
  leadsStore.leads.find(
    (l) =>
      l.vendorId === vendor.value?.id &&
      !TERMINAL_STATUSES.includes(
        l.status as (typeof TERMINAL_STATUSES)[number],
      ),
  ),
);

/** True when the couple had a past (cancelled/declined) lead but has no active one — allows re-enquiring. */
const canContactAgain = computed(
  () =>
    !existingLead.value &&
    leadsStore.leads.some(
      (l) =>
        l.vendorId === vendor.value?.id &&
        TERMINAL_STATUSES.includes(
          l.status as (typeof TERMINAL_STATUSES)[number],
        ),
    ),
);

const isFeatured = computed(
  () =>
    vendor.value?.tier === "PREMIUM" ||
    (vendor.value?.tier as string) === "STUDIO",
);

const ratingBars = computed(() => {
  if (!reviews.value.length) return [];
  return [5, 4, 3, 2, 1].map((n) => ({
    n,
    count: reviews.value.filter((r) => Math.round(r.rating) === n).length,
    pct: Math.round(
      (reviews.value.filter((r) => Math.round(r.rating) === n).length /
        reviews.value.length) *
        100,
    ),
  }));
});

const { saveEnquiryIntent, getEnquiryIntent } = useAuthRedirect();

function handleEnquire() {
  if (!authStore.isAuthenticated) {
    // Save full intent so we can resume after auth
    saveEnquiryIntent({
      vendorId: vendor.value!.id,
      vendorName: vendor.value!.businessName,
      vendorCategory: vendor.value!.category,
      vendorCity: vendor.value!.city,
      vendorPhotoUrl: vendor.value!.photos?.[0]?.url ?? "",
      packageInterest: null,
      returnPath: route.fullPath,
    });
    router.push({ path: "/login", query: { intent: "enquiry" } });
    return;
  }
  if (authStore.user?.role === "VENDOR") return;
  if (existingLead.value) {
    router.push(`/couple/enquiries`);
    return;
  }
  showModal.value = true;
}

onMounted(async () => {
  loading.value = true;
  try {
    const vendorRes = await vendorApi.get(route.params.id as string);
    vendor.value = vendorRes.data;
    // Load reviews, stats, and partners in parallel
    await Promise.allSettled([
      reviewsApi.getForVendor(route.params.id as string).then((r) => {
        reviews.value = r.data ?? [];
      }),
      vendorApi.getStats(route.params.id as string).then((r) => {
        avgResponseHours.value = r.data.avgResponseTimeHours;
      }),
      networkApi.getPublicPartners(route.params.id as string).then((r) => {
        partners.value = r.data ?? [];
      }),
      authStore.isAuthenticated
        ? leadsStore.fetchCoupleLeads()
        : Promise.resolve(),
    ]);
  } finally {
    loading.value = false;
  }

  // Auto-open modal when returning from auth with ?openEnquiry=true
  if (route.query.openEnquiry === "true") {
    // Clean URL immediately so back-button works correctly
    router.replace({ path: route.path });

    // Honour existing active lead — do not open modal redundantly
    if (existingLead.value) return;
    // Do allow re-opening when the previous lead was cancelled/declined

    // Restore package selection from saved intent (if any)
    const intent = getEnquiryIntent();
    if (intent?.packageInterest) {
      // EnquiryModal will receive this via prefilledPackage prop when we add it
    }

    // Small delay so the page renders before the modal appears
    setTimeout(() => {
      showModal.value = true;
    }, 400);
  }
});
</script>

<template>
  <div class="vp">
    <!-- Teleport for lightbox -->
    <Teleport to="body">
      <div v-if="lightboxPhoto" class="lightbox" @click="lightboxPhoto = null">
        <img :src="lightboxPhoto" class="lb-img" />
        <button class="lb-close" @click.stop="lightboxPhoto = null">
          <X :size="20" />
        </button>
      </div>
    </Teleport>

    <!-- Teleport for EnquiryModal -->
    <Teleport to="body">
      <EnquiryModal
        v-if="showModal && vendor"
        :vendor="vendor"
        :avg-response-hours="avgResponseHours ?? undefined"
        @close="showModal = false"
      />
    </Teleport>

    <!-- Nav -->
    <PublicNavbar />

    <!-- Loading / error -->
    <div v-if="loading" class="state-msg">
      <span class="spinner" /> {{ t("publicVendor.loading") }}
    </div>
    <div v-else-if="!vendor" class="state-msg">
      {{ t("publicVendor.notFound") }}
      <RouterLink to="/vendors">{{
        t("publicVendor.backToDirectory")
      }}</RouterLink>
    </div>

    <template v-else>
      <!-- ── Hero ── -->
      <div class="hero-strips">
        <template v-if="vendor.coverPhoto || vendor.photos?.length">
          <!-- Cover photo always first if set -->
          <div
            v-if="vendor.coverPhoto"
            class="hero-strip"
            :style="{ backgroundImage: `url('${vendor.coverPhoto}')` }"
          />
          <!-- Fill remaining strips from gallery (skip index 0 if coverPhoto is set to avoid duplicate) -->
          <div
            v-for="p in (vendor.coverPhoto
              ? vendor.photos
              : vendor.photos
            ).slice(0, vendor.coverPhoto ? 3 : 4)"
            :key="p.id"
            class="hero-strip"
            :style="{ backgroundImage: `url('${p.url}')` }"
          />
        </template>
        <div v-else class="hero-strip hero-strip-ph">
          {{ vendor.businessName?.[0] }}
        </div>
        <div class="hero-overlay" />
        <div class="hero-info">
          <div class="hero-badges">
            <span v-if="isFeatured" class="badge badge-featured"
              ><StarIcon :size="11" /> {{ t("publicVendor.featured") }}</span
            >
            <span class="badge badge-cat">{{
              vendor.category?.replace(/_/g, " ")
            }}</span>
          </div>
          <h1 class="hero-name">{{ vendor.businessName }}</h1>
          <p class="hero-meta">
            <span><MapPin :size="14" /> {{ vendor.city }}</span>
            <span v-if="vendor.averageRating">
              · ★ {{ vendor.averageRating.toFixed(1) }} ({{
                vendor.reviewCount
              }})</span
            >
            <span v-if="vendor.yearsExperience">
              · {{ vendor.yearsExperience }}
              {{ t("publicVendor.yearsExp") }}</span
            >
          </p>
        </div>
      </div>

      <!-- ── Content layout ── -->
      <div class="vp-body">
        <!-- Main column -->
        <div class="vp-main">
          <!-- About -->
          <section class="vp-section">
            <h2 class="sec-title">{{ t("publicVendor.about") }}</h2>
            <p v-if="vendor.description" class="about-text">
              {{ vendor.description }}
            </p>
            <p v-else class="about-text muted">
              {{ t("publicVendor.noDescription") }}
            </p>
            <div class="quick-facts">
              <div v-if="vendor.basePrice" class="qf">
                <span class="qf-label">{{
                  t("publicVendor.startingFrom")
                }}</span>
                <span class="qf-val"
                  >{{ vendor.basePrice.toLocaleString() }} RON</span
                >
              </div>
              <div v-if="vendor.yearsExperience" class="qf">
                <span class="qf-label">{{ t("publicVendor.experience") }}</span>
                <span class="qf-val">{{
                  t("publicVendor.yearsExperience", {
                    n: vendor.yearsExperience,
                  })
                }}</span>
              </div>
              <div v-if="vendor.languages?.length" class="qf">
                <span class="qf-label">{{ t("publicVendor.languages") }}</span>
                <span class="qf-val">{{ vendor.languages.join(", ") }}</span>
              </div>
              <div class="qf">
                <span class="qf-label">{{
                  t("publicVendor.responseTime")
                }}</span>
                <span class="qf-val">{{
                  formatResponseTime(avgResponseHours)
                }}</span>
              </div>
            </div>

            <!-- Social / web links -->
            <div
              v-if="vendor.website || vendor.instagram || vendor.facebook"
              class="social-links"
            >
              <a
                v-if="vendor.website"
                :href="vendor.website"
                target="_blank"
                rel="noopener"
                class="social-link link-web"
              >
                <Globe :size="14" /> Website
              </a>
              <a
                v-if="vendor.instagram"
                :href="
                  'https://instagram.com/' + vendor.instagram.replace('@', '')
                "
                target="_blank"
                rel="noopener"
                class="social-link link-ig"
              >
                <Camera :size="14" />
                {{
                  vendor.instagram.startsWith("@")
                    ? vendor.instagram
                    : "@" + vendor.instagram
                }}
              </a>
              <a
                v-if="vendor.facebook"
                :href="
                  vendor.facebook.startsWith('http')
                    ? vendor.facebook
                    : 'https://facebook.com/' + vendor.facebook
                "
                target="_blank"
                rel="noopener"
                class="social-link link-fb"
              >
                <Users :size="14" /> Facebook
              </a>
            </div>
          </section>

          <!-- Portfolio gallery -->
          <section v-if="vendor.photos?.length" class="vp-section">
            <h2 class="sec-title">{{ t("publicVendor.portfolio") }}</h2>
            <div class="gallery-grid">
              <div
                v-for="p in vendor.photos"
                :key="p.id"
                class="gallery-thumb"
                :style="{ backgroundImage: `url('${p.url}')` }"
                @click="lightboxPhoto = p.url"
              />
            </div>
          </section>

          <!-- Packages -->
          <section v-if="vendor.packages?.length" class="vp-section">
            <h2 class="sec-title">{{ t("publicVendor.packages") }}</h2>
            <div class="pkg-grid">
              <div
                v-for="pkg in vendor.packages"
                :key="pkg.id"
                class="pkg-card"
              >
                <div class="pkg-top">
                  <p class="pkg-name">{{ pkg.name }}</p>
                  <p class="pkg-price">{{ pkg.price?.toLocaleString() }} RON</p>
                </div>
                <p v-if="pkg.description" class="pkg-desc">
                  {{ pkg.description }}
                </p>
                <ul v-if="pkg.items?.length" class="pkg-items">
                  <li v-for="item in pkg.items" :key="item">✔ {{ item }}</li>
                </ul>
                <button class="pkg-enquire-btn" @click="handleEnquire">
                  {{ t("publicVendor.getQuote") }}
                </button>
              </div>
            </div>
          </section>

          <!-- Working With (Partner Network) -->
          <section v-if="partners.length" class="vp-section">
            <h2 class="sec-title">{{ t("publicVendor.workingWith") }}</h2>
            <p class="ww-subtitle">
              {{
                t("publicVendor.workingWithSub", { name: vendor.businessName })
              }}
            </p>
            <div class="ww-grid">
              <a
                v-for="p in partners"
                :key="p.id"
                class="ww-card"
                :href="p.onPlatform ? `/vendors/${p.partnerId}` : undefined"
                :class="{ 'ww-card-link': p.onPlatform }"
              >
                <div
                  class="ww-avatar"
                  :style="
                    p.partnerCoverPhoto
                      ? { backgroundImage: `url('${p.partnerCoverPhoto}')` }
                      : {}
                  "
                >
                  <span v-if="!p.partnerCoverPhoto">{{
                    p.partnerName.slice(0, 2).toUpperCase()
                  }}</span>
                </div>
                <p class="ww-name">{{ p.partnerName }}</p>
                <p v-if="p.partnerCategory" class="ww-cat">
                  {{ p.partnerCategory.replace(/_/g, " ") }}
                </p>
              </a>
            </div>
          </section>

          <!-- Reviews -->
          <section class="vp-section">
            <h2 class="sec-title">
              {{ t("publicVendor.reviews") }}
              <span class="review-count">({{ reviews.length }})</span>
            </h2>
            <div v-if="reviews.length" class="reviews-layout">
              <div class="rating-breakdown">
                <p class="avg-score">
                  {{ vendor.averageRating?.toFixed(1) ?? "-" }}
                </p>
                <p class="avg-label">★★★★★</p>
                <div v-for="bar in ratingBars" :key="bar.n" class="rb-row">
                  <span class="rb-n">{{ bar.n }}</span>
                  <div class="rb-track">
                    <div class="rb-fill" :style="{ width: bar.pct + '%' }" />
                  </div>
                  <span class="rb-count">{{ bar.count }}</span>
                </div>
              </div>
              <div class="reviews-list">
                <div v-for="r in reviews" :key="r.id" class="review-card">
                  <div class="review-header">
                    <span class="review-stars">{{ "★".repeat(r.rating) }}</span>
                    <span class="review-date">{{
                      new Date(r.createdAt).toLocaleDateString("en-GB")
                    }}</span>
                  </div>
                  <p class="review-text">{{ r.comment }}</p>
                  <div v-if="r.vendorReply" class="vendor-reply">
                    <span class="reply-label">{{
                      t("publicVendor.replyFrom", { name: vendor.businessName })
                    }}</span>
                    <p class="reply-text">{{ r.vendorReply }}</p>
                  </div>
                </div>
              </div>
            </div>
            <p v-else class="muted">
              {{
                t("publicVendor.noReviewsYet", { name: vendor.businessName })
              }}
            </p>
          </section>
        </div>

        <!-- Sticky aside -->
        <aside class="vp-aside">
          <div class="enquiry-panel">
            <div class="ep-header">
              <p v-if="vendor.basePrice" class="ep-price">
                {{ t("publicVendor.startingFrom") }}
                <strong>{{ vendor.basePrice.toLocaleString() }} RON</strong>
              </p>
              <p v-else class="ep-price-nr">
                {{ t("publicVendor.priceOnRequest") }}
              </p>
              <div v-if="vendor.averageRating" class="ep-rating">
                <span class="stars"
                  >★ {{ vendor.averageRating.toFixed(1) }}</span
                >
                <span class="rc"
                  >({{ vendor.reviewCount }}
                  {{
                    vendor.reviewCount !== 1
                      ? t("vendor.analytics.reviewPlural")
                      : t("vendor.analytics.reviewSingular")
                  }})</span
                >
              </div>
            </div>

            <div v-if="existingLead" class="existing-deal-box">
              <p>{{ t("publicVendor.existingEnquiry") }}</p>
              <button
                class="ep-btn ep-btn-sec"
                @click="router.push('/couple/enquiries')"
              >
                {{ t("publicVendor.viewEnquiries") }}
              </button>
            </div>
            <template v-else>
              <button class="ep-btn" @click="handleEnquire">
                {{
                  canContactAgain
                    ? t("publicVendor.contactAgain")
                    : t("publicVendor.sendEnquiryBtn")
                }}
              </button>
              <p class="ep-note">
                {{ t("publicVendor.noObligation") }}
                {{
                  !avgResponseHours ||
                  avgResponseHours <= 0 ||
                  avgResponseHours >= 24
                    ? t("publicVendor.respondTime24h")
                    : t("publicVendor.respondTimeTypically", {
                        time: formatResponseTime(
                          avgResponseHours,
                        ).toLowerCase(),
                      })
                }}
              </p>
            </template>

            <hr class="ep-sep" />
            <div class="ep-details">
              <div class="ep-detail">
                <MapPin :size="14" /><span>{{ vendor.city }}</span>
              </div>
              <div v-if="vendor.yearsExperience" class="ep-detail">
                <Clock :size="14" /><span>{{
                  t("publicVendor.yearsExperience", {
                    n: vendor.yearsExperience,
                  })
                }}</span>
              </div>
              <div class="ep-detail">
                <MessageSquare :size="14" /><span>{{
                  vendor.languages?.join(", ") ||
                  t("publicVendor.defaultLanguage")
                }}</span>
              </div>
              <div class="ep-detail">
                <Calendar :size="14" /><span>{{
                  t("publicVendor.checkAvailability")
                }}</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
.vp {
  min-height: 100vh;
  background: #f8f8f6;
  color: #1c1c1c;
}

/* Nav */
.vp-nav {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 58px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #eee;
}
.nav-back {
  font-size: 0.875rem;
  font-weight: 600;
  color: #c8974a;
  text-decoration: none;
}
.nav-logo {
  font-size: 1.3rem;
  font-weight: 900;
  color: #1c1c1c;
  text-decoration: none;
}
.nav-login {
  font-size: 0.875rem;
  color: #555;
  text-decoration: none;
  font-weight: 500;
}

/* State */
.state-msg {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
  padding: 80px;
  color: #888;
  font-size: 0.95rem;
}
.state-msg a {
  color: #c8974a;
  margin-left: 8px;
}
.spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid #e0e0e0;
  border-top-color: #c8974a;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Lightbox */
.lightbox {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.88);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.lb-img {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
  object-fit: contain;
}
.lb-close {
  position: absolute;
  top: 20px;
  right: 24px;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  color: #fff;
  font-size: 1.2rem;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
}

/* Hero strips */
.hero-strips {
  position: relative;
  height: 420px;
  display: flex;
  overflow: hidden;
}
.hero-strip {
  flex: 1;
  background-size: cover;
  background-position: center;
  transition: flex 0.3s;
}
.hero-strip-ph {
  background: #fdf8ee;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 6rem;
  font-weight: 900;
  color: #c8974a;
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.7) 0%,
    rgba(0, 0, 0, 0.1) 50%,
    transparent 100%
  );
  pointer-events: none;
}
.hero-info {
  position: absolute;
  bottom: 32px;
  left: 40px;
  right: 40px;
  color: #fff;
}
.hero-badges {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.badge {
  border-radius: 20px;
  padding: 4px 12px;
  font-size: 0.76rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.badge-featured {
  background: #c8974a;
  color: #fff;
}
.badge-cat {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  text-transform: capitalize;
}
.hero-name {
  font-size: clamp(1.6rem, 4vw, 2.6rem);
  font-weight: 900;
  letter-spacing: -0.5px;
  margin-bottom: 8px;
}
.hero-meta {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.8);
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* Body */
.vp-body {
  max-width: 1240px;
  margin: 0 auto;
  padding: 40px 24px;
  display: flex;
  gap: 36px;
  align-items: flex-start;
}
.vp-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* Sections */
.vp-section {
  background: #fff;
  border-radius: 16px;
  padding: 28px 28px;
  margin-bottom: 24px;
  border: 1px solid #eee;
}
.sec-title {
  font-size: 1.05rem;
  font-weight: 800;
  color: #1c1c1c;
  margin-bottom: 18px;
}
.review-count {
  color: #aaa;
  font-weight: 500;
}

/* About */
.about-text {
  font-size: 0.93rem;
  color: #555;
  line-height: 1.75;
  margin-bottom: 20px;
}
.muted {
  color: #aaa;
}
.quick-facts {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}
.qf {
  background: #fafaf8;
  border-radius: 10px;
  padding: 12px 14px;
  border: 1px solid #f0f0ee;
}
.qf-label {
  display: block;
  font-size: 0.72rem;
  font-weight: 700;
  color: #aaa;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}
.qf-val {
  font-size: 0.88rem;
  font-weight: 700;
  color: #333;
}
.social-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}
.social-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 0.83rem;
  font-weight: 600;
  text-decoration: none;
  border: 1.5px solid;
  transition: opacity 0.15s;
}
.social-link:hover {
  opacity: 0.75;
}
.link-web {
  color: #555;
  border-color: #ccc;
  background: #f7f7f7;
}
.link-ig {
  color: #c13584;
  border-color: #e0a0c8;
  background: #fdf0f8;
}
.link-fb {
  color: #1877f2;
  border-color: #a8c8f8;
  background: #f0f6ff;
}
.social-icon {
  font-size: 1rem;
}

/* Gallery */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}
.gallery-thumb {
  aspect-ratio: 4/3;
  border-radius: 8px;
  background-size: cover;
  background-position: center;
  cursor: pointer;
  transition: opacity 0.15s;
}
.gallery-thumb:hover {
  opacity: 0.85;
}

/* Packages */
.pkg-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.pkg-card {
  background: #fafaf8;
  border: 1.5px solid #eee;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pkg-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.pkg-name {
  font-weight: 800;
  font-size: 0.95rem;
  color: #1c1c1c;
}
.pkg-price {
  font-size: 0.95rem;
  font-weight: 800;
  color: #c8974a;
}
.pkg-desc {
  font-size: 0.82rem;
  color: #777;
  line-height: 1.55;
}
.pkg-items {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.pkg-items li {
  font-size: 0.82rem;
  color: #555;
}
.pkg-enquire-btn {
  margin-top: auto;
  background: transparent;
  border: 1.5px solid #c8974a;
  color: #c8974a;
  border-radius: 8px;
  padding: 9px;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition:
    background 0.15s,
    color 0.15s;
}
.pkg-enquire-btn:hover {
  background: #c8974a;
  color: #fff;
}

/* Reviews */
.reviews-layout {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}
.rating-breakdown {
  min-width: 140px;
  text-align: center;
  flex-shrink: 0;
}
.avg-score {
  font-size: 3rem;
  font-weight: 900;
  color: #1c1c1c;
  line-height: 1;
}
.avg-label {
  font-size: 1rem;
  color: #c8974a;
  margin-bottom: 14px;
}
.rb-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.rb-n {
  font-size: 0.75rem;
  color: #777;
  width: 10px;
  text-align: right;
}
.rb-track {
  flex: 1;
  height: 6px;
  background: #f0f0ee;
  border-radius: 3px;
  overflow: hidden;
}
.rb-fill {
  height: 100%;
  background: #c8974a;
  border-radius: 3px;
}
.rb-count {
  font-size: 0.72rem;
  color: #aaa;
  width: 16px;
}
.reviews-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.review-card {
  background: #fafaf8;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #f0f0ee;
}
.review-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.review-stars {
  color: #c8974a;
  font-size: 0.9rem;
}
.review-date {
  font-size: 0.75rem;
  color: #aaa;
}
.review-text {
  font-size: 0.87rem;
  color: #555;
  line-height: 1.65;
}
.vendor-reply {
  margin-top: 12px;
  padding: 10px 12px;
  background: #fdf8ee;
  border-left: 3px solid #c8974a;
  border-radius: 0 8px 8px 0;
}
.reply-label {
  display: block;
  font-size: 0.75rem;
  font-weight: 700;
  color: #c8974a;
  margin-bottom: 4px;
}
.reply-text {
  font-size: 0.83rem;
  color: #555;
}

/* Aside */
.vp-aside {
  width: 340px;
  flex-shrink: 0;
  position: sticky;
  top: 76px;
}
.enquiry-panel {
  background: #fff;
  border: 1.5px solid #eee;
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}
.ep-header {
  margin-bottom: 18px;
}
.ep-price {
  font-size: 1.1rem;
  color: #555;
}
.ep-price strong {
  font-size: 1.5rem;
  color: #1c1c1c;
  font-weight: 900;
}
.ep-price-nr {
  font-size: 0.9rem;
  color: #aaa;
}
.ep-rating {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}
.stars {
  font-size: 0.9rem;
  color: #c8974a;
  font-weight: 700;
}
.rc {
  font-size: 0.8rem;
  color: #aaa;
}
.existing-deal-box {
  background: #f0fff4;
  border: 1px solid #b7f0cc;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 14px;
  font-size: 0.85rem;
  color: #2d8a4e;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ep-btn {
  width: 100%;
  background: #c8974a;
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 14px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s;
  margin-bottom: 10px;
}
.ep-btn:hover {
  background: #b5833e;
}
.ep-btn-sec {
  background: #1c1c1c;
  margin-bottom: 0;
}
.ep-btn-sec:hover {
  background: #333;
}
.ep-note {
  font-size: 0.75rem;
  color: #aaa;
  text-align: center;
  margin-bottom: 16px;
}
.ep-sep {
  border: none;
  border-top: 1px solid #f0f0f0;
  margin: 16px 0;
}
.ep-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ep-detail {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.83rem;
  color: #555;
}

@media (max-width: 1000px) {
  .vp-body {
    flex-direction: column;
  }
  .vp-aside {
    width: 100%;
    position: static;
  }
}
@media (max-width: 600px) {
  .hero-strips {
    height: 240px;
  }
  .vp-body {
    padding: 16px;
  }
  .reviews-layout {
    flex-direction: column;
  }
  .pkg-grid {
    grid-template-columns: 1fr;
  }
}

/* ── Working With ──────────────────────────────────────────────────────────── */
.ww-subtitle {
  font-size: 0.83rem;
  color: #888;
  margin: 0 0 16px;
}
.ww-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.ww-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 100px;
  text-align: center;
  text-decoration: none;
  color: inherit;
  padding: 14px 10px;
  border: 1px solid #eee;
  border-radius: 12px;
  background: #fff;
  transition:
    box-shadow 0.15s,
    border-color 0.15s;
}
.ww-card-link:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.07);
  border-color: #ddd;
}
.ww-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #c8974a;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}
.ww-name {
  font-size: 0.78rem;
  font-weight: 600;
  color: #1c1c1c;
  line-height: 1.3;
}
.ww-cat {
  font-size: 0.7rem;
  color: #aaa;
  text-transform: capitalize;
}
</style>
