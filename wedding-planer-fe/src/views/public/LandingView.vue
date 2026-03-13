<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { vendorApi } from "@/api/vendor.api";
import VendorCard from "@/components/vendor/VendorCard.vue";
import PublicNavbar from "@/components/public/PublicNavbar.vue";
import type { VendorProfile } from "@/types/vendor.types";
import type { Component } from "vue";
import {
  Camera,
  Video,
  Landmark,
  Flower2,
  Utensils,
  Cake,
  Music,
  Headphones,
  Wand2,
  ClipboardList,
  MapPin,
  Search,
  BadgeCheck,
  Star,
  Inbox,
} from "lucide-vue-next";

const router = useRouter();
const { t } = useI18n();

const search = ref("");
const city = ref("");
const featuredVendors = ref<VendorProfile[]>([]);
const loadingFeatured = ref(false);
const featuredScrollRef = ref<HTMLElement | null>(null);

const categories = computed<{ key: string; label: string; icon: Component }[]>(
  () => [
    {
      key: "PHOTOGRAPHER",
      label: t("landing.categories.labels.PHOTOGRAPHER"),
      icon: Camera,
    },
    {
      key: "VIDEOGRAPHER",
      label: t("landing.categories.labels.VIDEOGRAPHER"),
      icon: Video,
    },
    {
      key: "VENUE",
      label: t("landing.categories.labels.VENUE"),
      icon: Landmark,
    },
    {
      key: "FLORIST",
      label: t("landing.categories.labels.FLORIST"),
      icon: Flower2,
    },
    {
      key: "CATERER",
      label: t("landing.categories.labels.CATERER"),
      icon: Utensils,
    },
    { key: "CAKE", label: t("landing.categories.labels.CAKE"), icon: Cake },
    { key: "BAND", label: t("landing.categories.labels.BAND"), icon: Music },
    { key: "DJ", label: t("landing.categories.labels.DJ"), icon: Headphones },
    {
      key: "MAKEUP_ARTIST",
      label: t("landing.categories.labels.MAKEUP_ARTIST"),
      icon: Wand2,
    },
    {
      key: "PLANNER",
      label: t("landing.categories.labels.PLANNER"),
      icon: ClipboardList,
    },
  ],
);

const steps = computed(() => [
  {
    n: "01",
    title: t("landing.howItWorks.steps.search.title"),
    body: t("landing.howItWorks.steps.search.body"),
  },
  {
    n: "02",
    title: t("landing.howItWorks.steps.explore.title"),
    body: t("landing.howItWorks.steps.explore.body"),
  },
  {
    n: "03",
    title: t("landing.howItWorks.steps.enquire.title"),
    body: t("landing.howItWorks.steps.enquire.body"),
  },
  {
    n: "04",
    title: t("landing.howItWorks.steps.book.title"),
    body: t("landing.howItWorks.steps.book.body"),
  },
]);

const testimonials = [
  {
    quote:
      "We booked our photographer, florist and band through Eternelle in under a week. The enquiry flow is just magical.",
    couple: "Adriana & Mihai",
    location: "Cluj-Napoca",
    initials: "AM",
  },
  {
    quote:
      "I was overwhelmed planning our wedding, but Eternelle made vendor discovery so easy. 10/10 would recommend.",
    couple: "Elena & Radu",
    location: "București",
    initials: "ER",
  },
  {
    quote:
      "Found our dream venue after just three enquiries. The platform is beautiful and the vendors are top-tier.",
    couple: "Ioana & Andrei",
    location: "Timișoara",
    initials: "IA",
  },
];

const whyItems = computed<{ icon: Component; title: string; body: string }[]>(
  () => [
    {
      icon: BadgeCheck,
      title: t("landing.why.items.verified.title"),
      body: t("landing.why.items.verified.body"),
    },
    {
      icon: Star,
      title: t("landing.why.items.reviews.title"),
      body: t("landing.why.items.reviews.body"),
    },
    {
      icon: Inbox,
      title: t("landing.why.items.messaging.title"),
      body: t("landing.why.items.messaging.body"),
    },
  ],
);

function search_go() {
  router.push({
    path: "/vendors",
    query: { q: search.value || undefined, city: city.value || undefined },
  });
}

function goCategory(cat: string) {
  router.push({ path: "/vendors", query: { category: cat } });
}

function scrollFeatured(direction: "left" | "right") {
  const container = featuredScrollRef.value;
  if (!container) return;

  const card = container.querySelector(".featured-card") as HTMLElement | null;
  const cardWidth = card?.offsetWidth ?? 320;
  const gap = 20;
  const offset = cardWidth + gap;

  container.scrollBy({
    left: direction === "right" ? offset : -offset,
    behavior: "smooth",
  });
}

onMounted(async () => {
  loadingFeatured.value = true;
  try {
    const res = await vendorApi.list({ tier: "PREMIUM" });
    featuredVendors.value = (res.data ?? []).slice(0, 8);
  } catch {
    // quietly fail
  } finally {
    loadingFeatured.value = false;
  }
});
</script>

<template>
  <div class="landing">
    <!-- Nav -->
    <PublicNavbar />

    <!-- ── Hero ── -->
    <section class="hero">
      <div class="hero-bg">
        <div class="hero-overlay" />
      </div>
      <div class="hero-content">
        <p class="hero-eyebrow">✦ Eternelle — {{ t("brand.tagline") }}</p>
        <h1 class="hero-heading">{{ t("landing.hero.title") }}</h1>
        <p class="hero-sub">
          {{ t("landing.hero.subtitle") }}
        </p>
        <form class="hero-search" @submit.prevent="search_go">
          <div class="hs-field">
            <span class="hs-icon"><Search :size="18" /></span>
            <input
              v-model="search"
              class="hs-input"
              type="text"
              :placeholder="t('landing.hero.searchPlaceholder')"
            />
          </div>
          <div class="hs-sep" />
          <div class="hs-field">
            <span class="hs-icon"><MapPin :size="18" /></span>
            <input
              v-model="city"
              class="hs-input"
              type="text"
              :placeholder="t('landing.hero.cityPlaceholder')"
            />
          </div>
          <button class="hs-btn" type="submit">
            {{ t("landing.hero.searchBtn") }}
          </button>
        </form>
        <p class="hero-hint">
          Trusted by <strong>2 400+</strong> {{ t("landing.hero.trustedBy") }}
        </p>
      </div>
    </section>

    <!-- ── Category Grid ── -->
    <section class="section category-section">
      <div class="container">
        <h2 class="section-title">{{ t("landing.categories.title") }}</h2>
        <p class="section-sub">
          {{ t("landing.categories.subtitle") }}
        </p>
        <div class="category-grid">
          <button
            v-for="cat in categories"
            :key="cat.key"
            class="cat-tile"
            @click="goCategory(cat.key)"
          >
            <span class="cat-icon"
              ><component :is="cat.icon" :size="28"
            /></span>
            <span class="cat-label">{{ cat.label }}</span>
          </button>
        </div>
        <button class="browse-all-btn" @click="router.push('/vendors')">
          {{ t("landing.categories.browseAll") }}
        </button>
      </div>
    </section>

    <!-- ── How It Works ── -->
    <section class="section hiw-section">
      <div class="container">
        <h2 class="section-title">{{ t("landing.howItWorks.title") }}</h2>
        <p class="section-sub">
          {{ t("landing.howItWorks.subtitle") }}
        </p>
        <div class="hiw-grid">
          <div v-for="step in steps" :key="step.n" class="hiw-card">
            <span class="hiw-n">{{ step.n }}</span>
            <h3 class="hiw-title">{{ step.title }}</h3>
            <p class="hiw-body">{{ step.body }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Featured Vendors ── -->
    <section
      v-if="!loadingFeatured && featuredVendors.length"
      class="section featured-section"
    >
      <div class="container">
        <div class="section-header-row">
          <div>
            <h2 class="section-title">{{ t("landing.featured.title") }}</h2>
            <p class="section-sub">
              {{ t("landing.featured.subtitle") }}
            </p>
          </div>
          <RouterLink to="/vendors" class="see-all-link"
            >{{ t("landing.featured.viewAll") }} →</RouterLink
          >
        </div>
        <div class="featured-carousel">
          <button
            class="featured-nav featured-nav-left"
            type="button"
            aria-label="Scroll featured vendors left"
            @click="scrollFeatured('left')"
          >
            ←
          </button>
          <div ref="featuredScrollRef" class="featured-scroll">
            <VendorCard
              v-for="v in featuredVendors"
              :key="v.id"
              :vendor="v"
              class="featured-card"
            />
          </div>
          <button
            class="featured-nav featured-nav-right"
            type="button"
            aria-label="Scroll featured vendors right"
            @click="scrollFeatured('right')"
          >
            →
          </button>
        </div>
      </div>
    </section>

    <!-- ── Why Eternelle ── -->
    <section class="section why-section">
      <div class="container">
        <h2 class="section-title center">{{ t("landing.why.title") }}</h2>
        <div class="why-grid">
          <div v-for="item in whyItems" :key="item.title" class="why-card">
            <span class="why-icon"
              ><component :is="item.icon" :size="24"
            /></span>
            <h3 class="why-title">{{ item.title }}</h3>
            <p class="why-body">{{ item.body }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Testimonials ── -->
    <section class="section testimonials-section">
      <div class="container">
        <h2 class="section-title center">
          {{ t("landing.testimonials.title") }}
        </h2>
        <div class="testi-grid">
          <div
            v-for="testi in testimonials"
            :key="testi.couple"
            class="testi-card"
          >
            <p class="testi-quote">"{{ testi.quote }}"</p>
            <div class="testi-author">
              <div class="testi-avatar">{{ testi.initials }}</div>
              <div>
                <p class="testi-name">{{ testi.couple }}</p>
                <p class="testi-loc">{{ testi.location }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Vendor CTA Banner ── -->
    <section class="vendor-banner">
      <div class="container vendor-banner-inner">
        <div>
          <h2 class="vb-title">{{ t("landing.hero.forVendors") }}</h2>
          <p class="vb-sub">
            {{ t("landing.vendorBanner.sub") }}
          </p>
        </div>
        <RouterLink to="/register/vendor" class="vb-btn"
          >{{ t("landing.hero.joinVendors") }} →</RouterLink
        >
      </div>
    </section>

    <!-- ── Footer ── -->
    <footer class="footer">
      <div class="container">
        <div class="footer-grid">
          <div class="footer-brand">
            <p class="footer-logo">Eternelle</p>
            <p class="footer-tagline">
              {{ t("landing.footer.tagline") }}
            </p>
          </div>
          <div class="footer-col">
            <p class="footer-col-title">{{ t("landing.footer.forCouples") }}</p>
            <RouterLink to="/vendors">{{
              t("landing.footer.findVendors")
            }}</RouterLink>
            <RouterLink to="/register/couple">{{
              t("landing.footer.createAccount")
            }}</RouterLink>
            <RouterLink to="/login">{{ t("common.signIn") }}</RouterLink>
          </div>
          <div class="footer-col">
            <p class="footer-col-title">{{ t("landing.footer.forVendors") }}</p>
            <RouterLink to="/register/vendor">{{
              t("landing.footer.joinEternelle")
            }}</RouterLink>
            <RouterLink to="/vendor/dashboard">{{
              t("landing.footer.dashboard")
            }}</RouterLink>
          </div>
          <div class="footer-col">
            <p class="footer-col-title">{{ t("landing.footer.categories") }}</p>
            <button
              v-for="cat in categories.slice(0, 5)"
              :key="cat.key"
              class="footer-link-btn"
              @click="goCategory(cat.key)"
            >
              {{ cat.label }}
            </button>
          </div>
        </div>
        <div class="footer-bottom">
          <p>
            © {{ new Date().getFullYear() }} Eternelle.
            {{ t("landing.footer.rights") }}
          </p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* Reset */
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
.landing {
  color: #1c1c1c;
  background: #fff;
}

/* Container */
.container {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ── Nav ── */
.nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}
.nav-logo {
  font-size: 1.5rem;
  font-weight: 900;
  color: #1c1c1c;
  text-decoration: none;
  letter-spacing: -0.5px;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 28px;
}
.nav-links a {
  color: #555;
  text-decoration: none;
  font-size: 0.9rem;
  transition: color 0.15s;
}
.nav-links a:hover {
  color: #1c1c1c;
}
.nav-login {
  font-weight: 500 !important;
}
.nav-cta {
  background: #1c1c1c;
  color: #fff !important;
  padding: 8px 18px;
  border-radius: 8px;
  font-weight: 600 !important;
  font-size: 0.85rem !important;
  transition: background 0.15s !important;
}
.nav-cta:hover {
  background: #333 !important;
}

/* ── Hero ── */
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background-image: url("../../../public/image.png");
  background-size: cover;
  background-position: center;
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(10, 4, 1, 0.52) 0%,
    rgba(15, 6, 1, 0.72) 100%
  );
}
.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 780px;
  padding: 0 24px;
  padding-top: 80px;
}
.hero-eyebrow {
  color: #c8974a;
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 20px;
}
.hero-heading {
  font-size: clamp(2.4rem, 6vw, 4.2rem);
  font-weight: 900;
  color: #fff;
  line-height: 1.1;
  letter-spacing: -1px;
  margin-bottom: 20px;
}
.hero-sub {
  color: rgba(255, 255, 255, 0.7);
  font-size: 1.05rem;
  line-height: 1.7;
  margin-bottom: 40px;
}
.hero-search {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 14px;
  padding: 6px;
  gap: 0;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.25);
  max-width: 680px;
  margin: 0 auto 20px;
}
.hs-field {
  display: flex;
  align-items: center;
  flex: 1;
  padding: 0 12px;
  gap: 8px;
}
.hs-icon {
  font-size: 1rem;
  color: #aaa;
}
.hs-input {
  border: none;
  outline: none;
  font-size: 0.9rem;
  background: transparent;
  width: 100%;
  color: #333;
}
.hs-input::placeholder {
  color: #bbb;
}
.hs-sep {
  width: 1px;
  height: 32px;
  background: #eee;
}
.hs-btn {
  background: #c8974a;
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 12px 22px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.hs-btn:hover {
  background: #b5833e;
}
.hero-hint {
  color: rgba(255, 255, 255, 0.45);
  font-size: 0.8rem;
}
.hero-hint strong {
  color: rgba(255, 255, 255, 0.7);
}

/* ── Sections ── */
.section {
  padding: 88px 0;
}
.section-title {
  font-size: 2rem;
  font-weight: 800;
  color: #1c1c1c;
  letter-spacing: -0.5px;
  margin-bottom: 10px;
}
.section-title.center {
  text-align: center;
}
.section-sub {
  color: #777;
  font-size: 0.95rem;
  margin-bottom: 48px;
}
.section-header-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 36px;
}
.see-all-link {
  color: #c8974a;
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
}

/* ── Category ── */
.category-section {
  background: #fafaf8;
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  margin-bottom: 32px;
}
.cat-tile {
  background: #fff;
  border: 1.5px solid #eee;
  border-radius: 14px;
  padding: 22px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition:
    border-color 0.15s,
    box-shadow 0.15s,
    transform 0.15s;
  font-family: inherit;
}
.cat-tile:hover {
  border-color: #c8974a;
  box-shadow: 0 4px 16px rgba(200, 151, 74, 0.15);
  transform: translateY(-2px);
}
.cat-icon {
  font-size: 1.6rem;
}
.cat-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #555;
  text-align: center;
}
.browse-all-btn {
  display: block;
  margin: 0 auto;
  background: transparent;
  border: 1.5px solid #c8974a;
  color: #c8974a;
  border-radius: 10px;
  padding: 10px 24px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition:
    background 0.15s,
    color 0.15s;
}
.browse-all-btn:hover {
  background: #c8974a;
  color: #fff;
}

/* ── How It Works ── */
.hiw-section {
  background: #fff;
}
.hiw-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 28px;
}
.hiw-card {
  background: #fdf8ee;
  border-radius: 14px;
  padding: 28px 24px;
  border: 1px solid #f0e8d0;
}
.hiw-n {
  display: block;
  font-size: 2.4rem;
  font-weight: 900;
  color: #e8d4b0;
  margin-bottom: 12px;
  letter-spacing: -1px;
}
.hiw-title {
  font-size: 1rem;
  font-weight: 700;
  color: #1c1c1c;
  margin-bottom: 8px;
}
.hiw-body {
  font-size: 0.85rem;
  color: #777;
  line-height: 1.65;
}

/* ── Featured ── */
.featured-section {
  background: #fafaf8;
}
.featured-carousel {
  position: relative;
}
.featured-scroll {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scroll-behavior: smooth;
  padding-bottom: 12px;
  scrollbar-width: none;
}
.featured-scroll::-webkit-scrollbar {
  display: none;
}
.featured-card {
  scroll-snap-align: start;
}
.featured-scroll :deep(.vendor-card) {
  flex: 0 0 380px;
  width: 380px;
  min-width: 380px;
}
.featured-nav {
  position: absolute;
  top: 45%;
  transform: translateY(-50%);
  z-index: 2;
  width: 36px;
  height: 36px;
  border: 1px solid #eadfc6;
  border-radius: 50%;
  background: #fff;
  color: #1c1c1c;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
}
.featured-nav:hover {
  border-color: #c8974a;
  color: #c8974a;
}
.featured-nav-left {
  left: -12px;
}
.featured-nav-right {
  right: -12px;
}

/* ── Why ── */
.why-section {
  background: #fff;
}
.why-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
}
.why-card {
  text-align: center;
  padding: 32px 24px;
}
.why-icon {
  display: block;
  font-size: 1.8rem;
  margin-bottom: 14px;
}
.why-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #1c1c1c;
  margin-bottom: 10px;
}
.why-body {
  font-size: 0.87rem;
  color: #777;
  line-height: 1.7;
}

/* ── Testimonials ── */
.testimonials-section {
  background: linear-gradient(135deg, #fdf8ee 0%, #fffdf8 100%);
}
.testi-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.testi-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  border: 1px solid #f0e8d0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.testi-quote {
  font-size: 0.9rem;
  color: #444;
  line-height: 1.7;
  margin-bottom: 20px;
  font-style: italic;
}
.testi-author {
  display: flex;
  align-items: center;
  gap: 12px;
}
.testi-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #c8974a;
  color: #fff;
  font-weight: 800;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.testi-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: #1c1c1c;
}
.testi-loc {
  font-size: 0.75rem;
  color: #aaa;
}

/* ── Vendor Banner ── */
.vendor-banner {
  background: #1c1c1c;
  padding: 72px 0;
}
.vendor-banner-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
}
.vb-title {
  font-size: 1.8rem;
  font-weight: 800;
  color: #fff;
  margin-bottom: 10px;
}
.vb-sub {
  color: rgba(255, 255, 255, 0.55);
  font-size: 0.9rem;
  line-height: 1.7;
  max-width: 560px;
}
.vb-btn {
  background: #c8974a;
  color: #fff;
  text-decoration: none;
  padding: 14px 28px;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.95rem;
  white-space: nowrap;
  transition: background 0.15s;
}
.vb-btn:hover {
  background: #b5833e;
}

/* ── Footer ── */
.footer {
  background: #111;
  padding: 60px 0 32px;
}
.footer-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 48px;
  margin-bottom: 48px;
}
.footer-logo {
  font-size: 1.4rem;
  font-weight: 900;
  color: #fff;
  margin-bottom: 12px;
}
.footer-tagline {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.35);
  line-height: 1.6;
  max-width: 220px;
}
.footer-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.footer-col-title {
  font-size: 0.78rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.35);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 4px;
}
.footer-col a {
  color: rgba(255, 255, 255, 0.55);
  text-decoration: none;
  font-size: 0.85rem;
  transition: color 0.15s;
}
.footer-col a:hover {
  color: #fff;
}
.footer-link-btn {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.55);
  font-size: 0.85rem;
  font-family: inherit;
  text-align: left;
  transition: color 0.15s;
}
.footer-link-btn:hover {
  color: #fff;
}
.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  padding-top: 24px;
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.25);
  text-align: center;
}

/* Responsive */
@media (max-width: 900px) {
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .hiw-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .testi-grid {
    grid-template-columns: 1fr 1fr;
  }
  .footer-grid {
    grid-template-columns: 1fr 1fr;
  }
  .vendor-banner-inner {
    flex-direction: column;
    text-align: center;
  }
  .featured-nav {
    display: none;
  }
  .featured-card {
    flex-basis: 320px;
  }
  .featured-scroll :deep(.vendor-card) {
    flex-basis: 320px;
    width: 320px;
    min-width: 320px;
  }
}
@media (max-width: 600px) {
  .nav {
    padding: 0 16px;
  }

  /* Hero */
  .hero {
    min-height: 100svh;
  }
  .hero-bg {
    background-position: 10% center;
  }
  .hero-content {
    padding: 80px 18px 48px;
    max-width: 100%;
  }
  .hero-eyebrow {
    font-size: 0.72rem;
    margin-bottom: 12px;
  }
  .hero-heading {
    font-size: clamp(1.8rem, 8vw, 2.6rem);
    letter-spacing: -0.5px;
    margin-bottom: 12px;
  }
  .hero-sub {
    font-size: 0.9rem;
    line-height: 1.6;
    margin-bottom: 24px;
  }

  /* Search form — vertical stack */
  .hero-search {
    flex-direction: column;
    padding: 10px;
    gap: 0;
    border-radius: 16px;
    max-width: 100%;
  }
  .hs-field {
    width: 100%;
    padding: 0 10px;
    min-height: 48px;
    box-sizing: border-box;
  }
  .hs-input {
    font-size: 0.95rem;
    padding: 12px 0;
    min-height: 44px;
  }
  .hs-sep {
    width: calc(100% - 20px);
    height: 1px;
    background: #f0f0f0;
    margin: 0 10px;
    flex-shrink: 0;
  }
  .hs-btn {
    width: 100%;
    min-height: 48px;
    font-size: 0.95rem;
    border-radius: 10px;
    margin-top: 8px;
  }
  .hero-hint {
    font-size: 0.72rem;
    margin-top: 12px;
  }

  /* Sections */
  .section {
    padding: 56px 0;
  }
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .hiw-grid {
    grid-template-columns: 1fr;
  }
  .why-grid {
    grid-template-columns: 1fr;
  }
  .testi-grid {
    grid-template-columns: 1fr;
  }
  .footer-grid {
    grid-template-columns: 1fr;
  }
  .section-header-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

@media (max-width: 380px) {
  .hero-heading {
    font-size: 1.65rem;
  }
  .hero-search {
    padding: 8px;
  }
  .hs-btn {
    padding: 13px;
  }
}
</style>
