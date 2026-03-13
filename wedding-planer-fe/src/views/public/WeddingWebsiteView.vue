<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from "vue";
import { useRoute } from "vue-router";
import { coupleApi } from "@/api/couple.api";
import { guestsApi } from "@/api/guests.api";
import type { PublicWeddingWebsite, RsvpRequest } from "@/types/couple.types";
import {
  MapPin,
  Calendar,
  Clock,
  Heart,
  ChevronDown,
  Eye,
} from "lucide-vue-next";

const route = useRoute();
const website = ref<PublicWeddingWebsite | null>(null);
const loading = ref(true);
const notFound = ref(false);
const isPreview = computed(() => route.query.preview === "true");

onMounted(async () => {
  try {
    const res = isPreview.value
      ? await coupleApi.previewWebsite()
      : await coupleApi.getPublicWebsite(route.params.subdomain as string);
    website.value = res.data;
  } catch {
    notFound.value = true;
  } finally {
    loading.value = false;
  }
  startCountdown();

  // If the URL contains ?guest=token, mark invite as opened and prefill the RSVP form.
  const token = route.query.guest as string | undefined;
  if (token && !isPreview.value) {
    guestToken.value = token;
    try {
      const g = await guestsApi.openInviteLink(token);
      if (g.data) {
        rsvp.value.firstName = g.data.firstName ?? "";
        rsvp.value.lastName = g.data.lastName ?? "";
        rsvp.value.email = g.data.email ?? "";
        rsvp.value.phone = g.data.phone ?? "";
      }
      // Scroll to RSVP form so the guest sees their pre-filled form immediately
      await nextTick();
      document.getElementById("rsvp")?.scrollIntoView({ behavior: "smooth" });
    } catch {
      /* invalid / used token – just ignore, form stays empty */
    }
  }
});

// ── Countdown ─────────────────────────────────────────────────────────────
const countdown = ref({ days: 0, hours: 0, minutes: 0, seconds: 0 });
let timer: ReturnType<typeof setInterval> | null = null;

function startCountdown() {
  function tick() {
    if (!website.value?.weddingDate) return;
    const target = new Date(website.value.weddingDate).getTime();
    const diff = target - Date.now();
    if (diff <= 0) {
      countdown.value = { days: 0, hours: 0, minutes: 0, seconds: 0 };
      return;
    }
    countdown.value = {
      days: Math.floor(diff / 86400000),
      hours: Math.floor((diff % 86400000) / 3600000),
      minutes: Math.floor((diff % 3600000) / 60000),
      seconds: Math.floor((diff % 60000) / 1000),
    };
  }
  timer = setInterval(tick, 1000);
  tick();
}

onUnmounted(() => {
  if (timer) clearInterval(timer);
});

const weddingDateFormatted = computed(() => {
  if (!website.value?.weddingDate) return "";
  return new Date(website.value.weddingDate).toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });
});

const isFuture = computed(() => {
  if (!website.value?.weddingDate) return false;
  return new Date(website.value.weddingDate).getTime() > Date.now();
});

const coupleNames = computed(() => {
  const p1 = website.value?.partner1Name ?? "";
  const p2 = website.value?.partner2Name ?? "";
  if (!p1 && !p2) return "";
  if (!p2) return p1;
  return `${p1} & ${p2}`;
});

// ── RSVP form ─────────────────────────────────────────────────────────────
const rsvp = ref<RsvpRequest>({
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  attending: "CONFIRMED",
  dietary: "NONE",
  dietaryNotes: "",
  plusOne: false,
  plusOneName: "",
  plusOneDietary: "NONE",
  plusOneDietaryNotes: "",
  message: "",
});

const rsvpSent = ref(false);
const rsvpError = ref("");
const rsvpLoading = ref(false);
const attending = ref<"CONFIRMED" | "DECLINED" | null>(null);
const guestToken = ref<string | null>(null);

async function submitRsvp() {
  if (!attending.value) {
    rsvpError.value = "Please select whether you're attending.";
    return;
  }
  if (!rsvp.value.firstName.trim()) {
    rsvpError.value = "Please enter your first name.";
    return;
  }
  rsvpError.value = "";
  rsvpLoading.value = true;
  try {
    await coupleApi.submitPublicRsvp(route.params.subdomain as string, {
      ...rsvp.value,
      attending: attending.value,
      inviteToken: guestToken.value ?? undefined,
    });
    rsvpSent.value = true;
  } catch (e: any) {
    rsvpError.value =
      e?.response?.data?.message ?? "Something went wrong. Please try again.";
  } finally {
    rsvpLoading.value = false;
  }
}
</script>

<template>
  <div class="ww">
    <!-- Loading -->
    <div v-if="loading" class="ww-loading">
      <div class="spinner"></div>
    </div>

    <!-- Preview banner (outside the v-if chain so main content still renders) -->
    <div v-if="isPreview && website" class="preview-banner">
      <Eye :size="14" /> Preview mode — this is exactly what your guests will
      see
    </div>

    <!-- Not found / unpublished -->
    <div v-if="!loading && notFound" class="ww-notfound">
      <Heart :size="48" class="nf-icon" />
      <h2>This page isn't available</h2>
      <p>
        The wedding website hasn't been published yet, or this link may have
        been removed.
      </p>
    </div>

    <!-- Main content -->
    <template v-else-if="!loading && website">
      <!-- HERO ─────────────────────────────────────────────────────── -->
      <section
        class="hero"
        :style="
          website.coverPhotoUrl
            ? `--hero-bg: url('${website.coverPhotoUrl}')`
            : ''
        "
      >
        <div class="hero-overlay"></div>
        <div class="hero-inner">
          <p class="hero-brand">Eternelle ♡</p>
          <h1 class="hero-names">
            {{ coupleNames || website.heroMessage || "Our Wedding" }}
          </h1>
          <p v-if="website.heroMessage && coupleNames" class="hero-tagline">
            "{{ website.heroMessage }}"
          </p>
          <div v-if="weddingDateFormatted" class="hero-meta">
            <span><Calendar :size="14" /> {{ weddingDateFormatted }}</span>
            <span v-if="website.weddingLocation"
              ><MapPin :size="14" /> {{ website.weddingLocation }}</span
            >
          </div>
        </div>
        <a class="hero-scroll" href="#countdown" v-if="isFuture"
          ><ChevronDown :size="22"
        /></a>
      </section>

      <!-- COUNTDOWN ────────────────────────────────────────────────── -->
      <section id="countdown" class="section countdown-section" v-if="isFuture">
        <div class="cd-grid">
          <div class="cd-unit">
            <span class="cd-num">{{ countdown.days }}</span
            ><span class="cd-label">Days</span>
          </div>
          <div class="cd-sep">:</div>
          <div class="cd-unit">
            <span class="cd-num">{{ countdown.hours }}</span
            ><span class="cd-label">Hours</span>
          </div>
          <div class="cd-sep">:</div>
          <div class="cd-unit">
            <span class="cd-num">{{ countdown.minutes }}</span
            ><span class="cd-label">Minutes</span>
          </div>
          <div class="cd-sep">:</div>
          <div class="cd-unit">
            <span class="cd-num">{{ countdown.seconds }}</span
            ><span class="cd-label">Seconds</span>
          </div>
        </div>
      </section>

      <!-- OUR STORY ────────────────────────────────────────────────── -->
      <section v-if="website.story" class="section story-section">
        <div class="section-inner">
          <div class="section-divider"><Heart :size="18" /></div>
          <h2 class="section-title">Our Story</h2>
          <p class="story-text">{{ website.story }}</p>
        </div>
      </section>

      <!-- EVENT DETAILS ────────────────────────────────────────────── -->
      <section
        v-if="website.ceremonyDate || website.receptionDate"
        class="section events-section"
      >
        <div class="section-inner">
          <div class="section-divider"><Calendar :size="18" /></div>
          <h2 class="section-title">The Day</h2>
          <div class="events-grid">
            <div v-if="website.ceremonyDate" class="event-card">
              <h3>Ceremony</h3>
              <p class="ev-detail">
                <Clock :size="13" /> {{ website.ceremonyDate }}
              </p>
              <p v-if="website.ceremonyLocation" class="ev-detail">
                <MapPin :size="13" /> {{ website.ceremonyLocation }}
              </p>
            </div>
            <div v-if="website.receptionDate" class="event-card">
              <h3>Reception</h3>
              <p class="ev-detail">
                <Clock :size="13" /> {{ website.receptionDate }}
              </p>
              <p v-if="website.receptionLocation" class="ev-detail">
                <MapPin :size="13" /> {{ website.receptionLocation }}
              </p>
            </div>
          </div>
        </div>
      </section>

      <!-- RSVP ────────────────────────────────────────────────────── -->
      <section class="section rsvp-section" id="rsvp">
        <div class="section-inner rsvp-inner">
          <div class="section-divider"><Heart :size="18" /></div>
          <h2 class="section-title">RSVP</h2>
          <p class="rsvp-sub">
            We'd love to celebrate with you. Please let us know if you can make
            it.
          </p>

          <!-- Success state -->
          <div v-if="rsvpSent" class="rsvp-success">
            <Heart :size="40" class="rsvp-success-icon" />
            <h3>
              {{
                attending === "CONFIRMED"
                  ? "We can't wait to see you! 🎉"
                  : "We'll miss you! 💔"
              }}
            </h3>
            <p>
              {{
                attending === "CONFIRMED"
                  ? "Your RSVP has been received. See you there!"
                  : "Thank you for letting us know."
              }}
            </p>
          </div>

          <!-- RSVP Form -->
          <form v-else class="rsvp-form" @submit.prevent="submitRsvp">
            <!-- Attendance buttons -->
            <div class="attend-btns">
              <button
                type="button"
                class="attend-btn"
                :class="{ active: attending === 'CONFIRMED' }"
                @click="attending = 'CONFIRMED'"
              >
                ✓ Joyfully accepts
              </button>
              <button
                type="button"
                class="attend-btn decline"
                :class="{ active: attending === 'DECLINED' }"
                @click="attending = 'DECLINED'"
              >
                ✗ Regretfully declines
              </button>
            </div>

            <div class="rsvp-fields">
              <div class="rsvp-row">
                <div class="rsvp-field">
                  <label>First Name *</label>
                  <input
                    v-model="rsvp.firstName"
                    class="ri"
                    placeholder="Elena"
                    required
                  />
                </div>
                <div class="rsvp-field">
                  <label>Last Name</label>
                  <input
                    v-model="rsvp.lastName"
                    class="ri"
                    placeholder="Popescu"
                  />
                </div>
              </div>
              <div class="rsvp-row">
                <div class="rsvp-field">
                  <label>Email</label>
                  <input
                    v-model="rsvp.email"
                    class="ri"
                    type="email"
                    placeholder="elena@email.com"
                  />
                </div>
                <div class="rsvp-field">
                  <label>Phone</label>
                  <input
                    v-model="rsvp.phone"
                    class="ri"
                    placeholder="+40 7XX XXX XXX"
                  />
                </div>
              </div>

              <template v-if="attending === 'CONFIRMED'">
                <!-- Dietary -->
                <div class="rsvp-field">
                  <label>Dietary Preference</label>
                  <select v-model="rsvp.dietary" class="ri">
                    <option value="NONE">No restrictions</option>
                    <option value="VEGAN">Vegan</option>
                    <option value="VEGETARIAN">Vegetarian</option>
                    <option value="GLUTEN_FREE">Gluten-free</option>
                    <option value="HALAL">Halal</option>
                    <option value="KOSHER">Kosher</option>
                    <option value="OTHER">Other (specify below)</option>
                  </select>
                </div>
                <div v-if="rsvp.dietary === 'OTHER'" class="rsvp-field">
                  <label>Please specify</label>
                  <input
                    v-model="rsvp.dietaryNotes"
                    class="ri"
                    placeholder="Details…"
                  />
                </div>

                <!-- +1 -->
                <label class="check-row">
                  <input type="checkbox" v-model="rsvp.plusOne" />
                  <span>I'm bringing a +1</span>
                </label>
                <div v-if="rsvp.plusOne" class="rsvp-field">
                  <label>+1 Name</label>
                  <input
                    v-model="rsvp.plusOneName"
                    class="ri"
                    placeholder="Guest's name"
                  />
                </div>
                <template v-if="rsvp.plusOne">
                  <div class="rsvp-field">
                    <label>+1 Dietary Preference</label>
                    <select v-model="rsvp.plusOneDietary" class="ri">
                      <option value="NONE">No restrictions</option>
                      <option value="VEGAN">Vegan</option>
                      <option value="VEGETARIAN">Vegetarian</option>
                      <option value="GLUTEN_FREE">Gluten-free</option>
                      <option value="HALAL">Halal</option>
                      <option value="KOSHER">Kosher</option>
                      <option value="OTHER">Other (specify below)</option>
                    </select>
                  </div>
                  <div
                    v-if="rsvp.plusOneDietary === 'OTHER'"
                    class="rsvp-field"
                  >
                    <label>+1 Dietary Notes</label>
                    <input
                      v-model="rsvp.plusOneDietaryNotes"
                      class="ri"
                      placeholder="Details…"
                    />
                  </div>
                </template>

              <!-- Message -->
              <div class="rsvp-field">
                <label>Message to the couple</label>
                <textarea
                  v-model="rsvp.message"
                  class="ri"
                  rows="3"
                  placeholder="Congratulations, we're so excited! 🥂"
                ></textarea>
              </div>
            </div>

            <p v-if="rsvpError" class="rsvp-err">{{ rsvpError }}</p>
            <button type="submit" class="rsvp-submit" :disabled="rsvpLoading">
              {{ rsvpLoading ? "Sending…" : "Send RSVP" }}
            </button>
          </form>
        </div>
      </section>

      <!-- FOOTER ──────────────────────────────────────────────────── -->
      <footer class="ww-footer">
        <p>Made with ♡ on <strong>Eternelle</strong></p>
      </footer>
    </template>
  </div>
</template>

<style scoped>
* {
  box-sizing: border-box;
}
.ww {
  min-height: 100vh;
  background: #fffdf9;
  font-family: Georgia, "Times New Roman", serif;
  color: #2d2d2d;
}

/* Loading / Not found */
.ww-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e5e5e5;
  border-top-color: #c9a96e;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.ww-notfound {
  text-align: center;
  padding: 96px 24px;
}
.preview-banner {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  background: #2d2d2d;
  color: #f0d9c8;
  font-size: 0.8rem;
  font-weight: 500;
  padding: 8px 16px;
  letter-spacing: 0.02em;
}
.nf-icon {
  color: #e5c5b0;
  margin-bottom: 16px;
}
.ww-notfound h2 {
  font-size: 1.6rem;
  margin: 0 0 8px;
}
.ww-notfound p {
  color: #888;
}

/* HERO */
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-image: var(
    --hero-bg,
    linear-gradient(135deg, #1a0a00 0%, #3d1f0b 50%, #1a0a00 100%)
  );
  background-size: cover;
  background-position: center;
  color: #fff;
  text-align: center;
  padding: 80px 24px;
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.45) 0%,
    rgba(0, 0, 0, 0.6) 100%
  );
}
.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 700px;
}
.hero-brand {
  font-size: 1rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  opacity: 0.8;
  margin: 0 0 24px;
  font-family: sans-serif;
}
.hero-names {
  font-size: clamp(2.5rem, 7vw, 5rem);
  font-weight: 400;
  margin: 0 0 16px;
  letter-spacing: 0.02em;
  line-height: 1.1;
}
.hero-tagline {
  font-style: italic;
  font-size: 1.15rem;
  opacity: 0.9;
  margin: 0 0 28px;
}
.hero-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
  font-size: 0.95rem;
  opacity: 0.9;
  font-family: sans-serif;
}
.hero-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
}
.hero-scroll {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1;
  color: rgba(255, 255, 255, 0.7);
  animation: bob 2s ease-in-out infinite;
  text-decoration: none;
}
@keyframes bob {
  0%,
  100% {
    transform: translateX(-50%) translateY(0);
  }
  50% {
    transform: translateX(-50%) translateY(8px);
  }
}

/* SECTIONS */
.section {
  padding: 80px 24px;
}
.section-inner {
  max-width: 760px;
  margin: 0 auto;
  text-align: center;
}
.section-divider {
  color: #c9a96e;
  margin-bottom: 16px;
}
.section-title {
  font-size: 2rem;
  font-weight: 400;
  margin: 0 0 24px;
  letter-spacing: 0.03em;
}

/* COUNTDOWN */
.countdown-section {
  background: #2d2d2d;
  color: #fff;
}
.cd-grid {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.cd-unit {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 72px;
}
.cd-num {
  font-size: clamp(2.5rem, 5vw, 4rem);
  font-weight: 700;
  line-height: 1;
}
.cd-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  opacity: 0.7;
  font-family: sans-serif;
  margin-top: 4px;
}
.cd-sep {
  font-size: 3rem;
  opacity: 0.5;
  padding-bottom: 20px;
}

/* STORY */
.story-section {
  background: #fffdf9;
}
.story-text {
  font-size: 1.05rem;
  line-height: 1.9;
  color: #4a3728;
  white-space: pre-line;
  max-width: 640px;
  margin: 0 auto;
}

/* EVENTS */
.events-section {
  background: #f7f0e8;
}
.events-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-top: 8px;
}
@media (max-width: 600px) {
  .events-grid {
    grid-template-columns: 1fr;
  }
}
.event-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px;
  text-align: left;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.event-card h3 {
  margin: 0 0 14px;
  font-size: 1.3rem;
  font-weight: 400;
  color: #c9a96e;
}
.ev-detail {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin: 6px 0;
  font-size: 0.9rem;
  color: #555;
  font-family: sans-serif;
  line-height: 1.4;
}

/* RSVP */
.rsvp-section {
  background: #fffdf9;
}
.rsvp-inner {
  max-width: 620px;
}
.rsvp-sub {
  color: #777;
  font-family: sans-serif;
  margin: -12px 0 32px;
  font-size: 0.95rem;
}
.rsvp-success {
  text-align: center;
  padding: 32px 16px;
}
.rsvp-success-icon {
  color: #c9a96e;
  margin-bottom: 16px;
}
.rsvp-success h3 {
  font-size: 1.5rem;
  margin: 0 0 8px;
}
.rsvp-success p {
  color: #777;
  font-family: sans-serif;
}

.rsvp-form {
  text-align: left;
}
.attend-btns {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
  justify-content: center;
}
.attend-btn {
  flex: 1;
  max-width: 220px;
  padding: 14px 16px;
  border: 2px solid #e0d5c8;
  border-radius: 10px;
  font-size: 0.9rem;
  font-family: Georgia, serif;
  cursor: pointer;
  background: #fff;
  color: #555;
  transition: all 0.2s;
}
.attend-btn.active {
  border-color: #c9a96e;
  background: #fdf6ec;
  color: #2d2d2d;
  font-weight: 700;
}
.attend-btn.decline.active {
  border-color: #e08080;
  background: #fff5f5;
}

.rsvp-fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.rsvp-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 520px) {
  .rsvp-row {
    grid-template-columns: 1fr;
  }
}
.rsvp-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.rsvp-field label {
  font-size: 0.8rem;
  font-family: sans-serif;
  color: #888;
  display: flex;
  align-items: center;
  gap: 4px;
}
.ri {
  padding: 11px 14px;
  border: 1.5px solid #e0d5c8;
  border-radius: 8px;
  font-size: 0.9rem;
  font-family: inherit;
  outline: none;
  background: #fff;
}
.ri:focus {
  border-color: #c9a96e;
}
.check-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: sans-serif;
  font-size: 0.9rem;
  cursor: pointer;
  color: #555;
}
.rsvp-err {
  color: #e05656;
  font-family: sans-serif;
  font-size: 0.85rem;
  text-align: center;
  margin: 8px 0;
}
.rsvp-submit {
  width: 100%;
  margin-top: 12px;
  padding: 14px;
  background: #c9a96e;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-family: Georgia, serif;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: background 0.2s;
}
.rsvp-submit:hover:not(:disabled) {
  background: #b8934a;
}
.rsvp-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* FOOTER */
.ww-footer {
  background: #2d2d2d;
  color: rgba(255, 255, 255, 0.7);
  text-align: center;
  padding: 24px;
  font-family: sans-serif;
  font-size: 0.85rem;
}
.ww-footer strong {
  color: #c9a96e;
}
</style>
