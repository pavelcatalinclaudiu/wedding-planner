<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { coupleApi } from "@/api/couple.api";
import type { WeddingWebsite } from "@/types/couple.types";

const route = useRoute();
const { t } = useI18n();
const website = ref<WeddingWebsite | null>(null);
const loading = ref(false);
const notFound = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    website.value = await coupleApi.getPublicWebsite(
      route.params.slug as string,
    );
  } catch {
    notFound.value = true;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="wedding-website">
    <div v-if="loading" class="loading">{{ t("common.loading") }}</div>
    <div v-else-if="notFound" class="not-found">
      <h2>{{ t("website.notFound") }}</h2>
      <p>{{ t("website.notFoundDesc") }}</p>
    </div>
    <template v-else-if="website">
      <div class="site-hero">
        <p class="site-logo">Eternelle ♡</p>
        <h1 class="couple-names">{{ website.heroMessage }}</h1>
        <p v-if="website.weddingDate" class="site-date">
          {{
            new Date(website.weddingDate).toLocaleDateString("en-GB", {
              day: "numeric",
              month: "long",
              year: "numeric",
            })
          }}
        </p>
        <p v-if="website.location" class="site-location">
          📍 {{ website.location }}
        </p>
      </div>

      <div class="site-body">
        <section v-if="website.story" class="site-section">
          <h2>{{ t("website.ourStorySection") }}</h2>
          <p class="story-text">{{ website.story }}</p>
        </section>

        <section class="rsvp-section">
          <h2>{{ t("website.rsvpSection") }}</h2>
          <p>
            {{ t("website.rsvpDesc") }}
          </p>
          <RouterLink :to="`/rsvp/${route.params.slug}`" class="rsvp-btn">{{
            t("website.rsvpBtn")
          }}</RouterLink>
        </section>
      </div>

      <footer class="site-footer">
        <p>{{ t("website.madeWith") }} <strong>Eternelle</strong></p>
      </footer>
    </template>
  </div>
</template>

<style scoped>
.wedding-website {
  min-height: 100vh;
  background: #fffdf9;
  font-family: Georgia, serif;
}
.loading {
  text-align: center;
  padding: 64px;
  color: #999;
}
.not-found {
  text-align: center;
  padding: 96px 24px;
}
.not-found h2 {
  margin: 0 0 12px;
}
.not-found p {
  color: #999;
}
.site-hero {
  background: linear-gradient(135deg, #1a1a1a 0%, #2c2c2c 100%);
  color: #fff;
  padding: 96px 24px;
  text-align: center;
}
.site-logo {
  margin: 0 0 24px;
  font-size: 1.2rem;
  opacity: 0.7;
}
.couple-names {
  margin: 0 0 18px;
  font-size: clamp(2rem, 5vw, 3.5rem);
  font-weight: 400;
  letter-spacing: 0.02em;
}
.site-date {
  margin: 0 0 8px;
  font-size: 1.2rem;
  opacity: 0.9;
}
.site-location {
  margin: 0;
  font-size: 1rem;
  opacity: 0.75;
  font-style: italic;
}
.site-body {
  max-width: 720px;
  margin: 0 auto;
  padding: 64px 24px;
}
.site-section {
  margin-bottom: 56px;
}
.site-section h2 {
  font-size: 1.6rem;
  font-weight: 400;
  margin: 0 0 20px;
  color: #2c2c2c;
  border-bottom: 1px solid #e8e4de;
  padding-bottom: 10px;
}
.story-text {
  font-size: 1.05rem;
  line-height: 1.8;
  color: #444;
}
.rsvp-section {
  text-align: center;
  padding: 48px 24px;
  background: #fdf8ee;
  border-radius: 16px;
}
.rsvp-section h2 {
  font-size: 1.6rem;
  font-weight: 400;
  margin: 0 0 12px;
}
.rsvp-section p {
  color: #666;
  margin: 0 0 24px;
}
.rsvp-btn {
  display: inline-block;
  background: #b8860b;
  color: #fff;
  text-decoration: none;
  border-radius: 8px;
  padding: 14px 36px;
  font-size: 1rem;
  font-weight: 700;
  font-family: sans-serif;
}
.site-footer {
  text-align: center;
  padding: 32px;
  color: #aaa;
  font-family: sans-serif;
  font-size: 0.85rem;
  border-top: 1px solid #f0ede8;
}
</style>
