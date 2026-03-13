<script setup lang="ts">
import { computed, onMounted } from "vue";
import {
  differenceInCalendarDays,
  differenceInMinutes,
  differenceInHours,
  differenceInDays,
  parseISO,
} from "date-fns";
import { useI18n } from "vue-i18n";
import { useCoupleStore } from "@/stores/couple.store";
import { useLeadsStore } from "@/stores/leads.store";
import { useChecklistStore } from "@/stores/checklist.store";
import { useBudgetStore } from "@/stores/budget.store";
import { useGuestsStore } from "@/stores/guests.store";
import { useMessagesStore } from "@/stores/messages.store";
import type { Component } from "vue";
import {
  Camera,
  Landmark,
  Utensils,
  Flower2,
  Music,
  Video,
  Cake,
  Car,
  Scissors,
  Lightbulb,
  Mail,
  Gem,
  Tag,
  MapPin,
  MessageCircle,
  AlertTriangle,
} from "lucide-vue-next";

import { useCategoryLabel } from "@/utils/vendorCategories";

const { t, locale } = useI18n();

const coupleStore = useCoupleStore();
const leadsStore = useLeadsStore();
const checklistStore = useChecklistStore();
const budgetStore = useBudgetStore();
const guestsStore = useGuestsStore();
const messagesStore = useMessagesStore();

const { categoryLabel } = useCategoryLabel();

onMounted(async () => {
  await Promise.all([
    coupleStore.profile || coupleStore.fetchProfile(),
    checklistStore.items.length === 0 && checklistStore.fetchChecklist(),
    budgetStore.items.length === 0 && budgetStore.fetchBudget(),
    guestsStore.guests.length === 0 && guestsStore.fetchGuests(),
    leadsStore.leads.length === 0 && leadsStore.fetchCoupleLeads(),
    messagesStore.threads.length === 0 && messagesStore.fetchThreads(),
  ]);
});

// ── Banner ─────────────────────────────────────────────────────────────────
const partner1 = computed(() => coupleStore.profile?.partner1Name ?? "");
const partner2 = computed(() => coupleStore.profile?.partner2Name ?? "");
const weddingLocation = computed(
  () => coupleStore.profile?.weddingLocation ?? "",
);
const weddingDate = computed(() => coupleStore.profile?.weddingDate ?? null);

// Use profile date if set; otherwise fall back to the first booked lead's eventDate
const effectiveWeddingDate = computed(() => {
  if (weddingDate.value) return weddingDate.value;
  const bookedLead = leadsStore.leads.find((l) => l.status === "BOOKED");
  return bookedLead?.eventDate ?? null;
});

// Use profile location if set; otherwise fall back to the booked vendor's city
const effectiveWeddingLocation = computed(() => {
  if (weddingLocation.value) return weddingLocation.value;
  const bookedLead = leadsStore.leads.find((l) => l.status === "BOOKED");
  return bookedLead?.vendorCity ?? "";
});

// Show wedding details in the hero banner when we have a date (from profile or booking)
const showWeddingDetails = computed(() => !!effectiveWeddingDate.value);

const daysToGo = computed(() => {
  if (!effectiveWeddingDate.value) return null;
  const diff = differenceInCalendarDays(
    parseISO(effectiveWeddingDate.value),
    new Date(),
  );
  return diff > 0 ? diff : null;
});

const formattedWeddingDate = computed(() => {
  if (!effectiveWeddingDate.value) return "";
  return new Date(effectiveWeddingDate.value).toLocaleDateString(
    locale.value === "ro" ? "ro-RO" : "en-GB",
    {
      day: "numeric",
      month: "long",
      year: "numeric",
    },
  );
});

// ── Widgets ────────────────────────────────────────────────────────────────
const tasksDone = computed(() => checklistStore.done);
const tasksTotal = computed(() => checklistStore.total);

const budgetTotal = computed(() =>
  Number(coupleStore.profile?.totalBudget ?? 0),
);
const budgetSpent = computed(() => budgetStore.totalActual);
const budgetRemaining = computed(() => budgetTotal.value - budgetSpent.value);

const guestsConfirmed = computed(() => guestsStore.confirmed);
const guestsPending = computed(() => guestsStore.pending);

const vendorsBooked = computed(
  () => leadsStore.leads.filter((l) => l.status === "BOOKED").length,
);
const vendorsPendingReply = computed(
  () =>
    leadsStore.leads.filter((l) => ["NEW", "IN_DISCUSSION"].includes(l.status))
      .length,
);

// ── Upcoming Tasks ─────────────────────────────────────────────────────────
const upcomingTasks = computed(() => {
  return [...checklistStore.items]
    .sort((a, b) => {
      if (a.done !== b.done) return a.done ? 1 : -1;
      if (!a.dueDate) return 1;
      if (!b.dueDate) return -1;
      return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
    })
    .slice(0, 5);
});

function dueDateLabel(item: { done: boolean; dueDate?: string }): string {
  if (item.done) return t("common.completed");
  if (!item.dueDate) return "";
  const diff = differenceInCalendarDays(parseISO(item.dueDate), new Date());
  if (diff < 0) return t("checklist.filters.overdue");
  if (diff === 0) return locale.value === "ro" ? "Scadent azi" : "Due today";
  if (diff <= 7)
    return locale.value === "ro"
      ? `În ${diff} ${diff === 1 ? "zi" : "zile"}`
      : `Due in ${diff} day${diff === 1 ? "" : "s"}`;
  return new Date(item.dueDate).toLocaleDateString(
    locale.value === "ro" ? "ro-RO" : "en-GB",
    { day: "numeric", month: "short" },
  );
}

function dueDateUrgent(item: { done: boolean; dueDate?: string }): boolean {
  if (item.done || !item.dueDate) return false;
  return differenceInCalendarDays(parseISO(item.dueDate), new Date()) <= 7;
}

// ── Wedding Team ───────────────────────────────────────────────────────────
const categoryIcons: Record<string, Component> = {
  PHOTOGRAPHY: Camera,
  VENUE: Landmark,
  CATERING: Utensils,
  FLORALS: Flower2,
  MUSIC: Music,
  VIDEOGRAPHY: Video,
  CAKE: Cake,
  TRANSPORT: Car,
  HAIR_MAKEUP: Scissors,
  LIGHTING: Lightbulb,
  STATIONERY: Mail,
  JEWELLERY: Gem,
};

function categoryIcon(cat?: string): Component {
  return cat ? (categoryIcons[cat] ?? Tag) : Tag;
}

function statusBadgeClass(status: string): string {
  if (status === "BOOKED") return "badge-booked";
  if (status === "CANCELLED") return "badge-cancelled";
  if (status === "IN_DISCUSSION" || status === "QUOTED") return "badge-pending";
  return "badge-contacted";
}

function statusLabel(status: string): string {
  if (status === "BOOKED") return t("common.booked");
  if (status === "CANCELLED")
    return locale.value === "ro" ? "Anulat" : "Cancelled";
  if (status === "IN_DISCUSSION") return t("common.pending");
  if (status === "QUOTED") return locale.value === "ro" ? "Cotat" : "Quoted";
  return locale.value === "ro" ? "Contactat" : "Contacted";
}

const teamLeads = computed(() =>
  [...leadsStore.leads]
    .filter((l) => !["DECLINED", "CANCELLED"].includes(l.status))
    .sort((a, b) => {
      const order: Record<string, number> = {
        BOOKED: 0,
        QUOTED: 1,
        IN_DISCUSSION: 2,
        NEW: 3,
      };
      return (order[a.status] ?? 9) - (order[b.status] ?? 9);
    })
    .slice(0, 4),
);

// ── Budget Overview ────────────────────────────────────────────────────────
const budgetByCategory = computed(() => {
  const map: Record<string, number> = {};
  budgetStore.items.forEach((item) => {
    const cat = item.category ?? "Other";
    map[cat] =
      (map[cat] ?? 0) + Number(item.actualCost ?? item.estimatedCost ?? 0);
  });
  return Object.entries(map)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 4);
});

function budgetBarWidth(amount: number): string {
  if (!budgetSpent.value) return "0%";
  return `${Math.min(100, Math.round((amount / budgetSpent.value) * 100))}%`;
}

function formatEur(n: number): string {
  if (n >= 1000) return `€${(n / 1000).toFixed(n % 1000 === 0 ? 0 : 1)}K`;
  return `€${n.toLocaleString()}`;
}

// ── Recent Messages ────────────────────────────────────────────────────────
const recentThreads = computed(() =>
  [...messagesStore.threads]
    .sort((a, b) => {
      const ta = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0;
      const tb = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0;
      return tb - ta;
    })
    .slice(0, 4),
);

function timeAgo(dateStr?: string): string {
  if (!dateStr) return "";
  const date = parseISO(dateStr);
  const mins = differenceInMinutes(new Date(), date);
  if (mins < 60)
    return locale.value === "ro" ? `acum ${mins}m` : `${mins}m ago`;
  const hrs = differenceInHours(new Date(), date);
  if (hrs < 24) return locale.value === "ro" ? `acum ${hrs}h` : `${hrs}h ago`;
  const days = differenceInDays(new Date(), date);
  if (days === 1) return locale.value === "ro" ? "Ieri" : "Yesterday";
  return locale.value === "ro"
    ? `acum ${days} ${days === 1 ? "zi" : "zile"}`
    : `${days} day${days === 1 ? "" : "s"} ago`;
}
</script>

<template>
  <div class="overview">
    <!-- ── Hero Banner ──────────────────────────────────────────── -->
    <div class="hero-banner">
      <div class="hero-left">
        <p class="hero-welcome">{{ t("overview.welcomeBack") }}</p>
        <h1 class="hero-title">
          <span class="hero-names"
            >{{ partner1
            }}<template v-if="partner2"> &amp; {{ partner2 }}</template
            >'s</span
          ><br />
          <span class="hero-subtitle">{{ t("overview.perfectDay") }}</span>
        </h1>
        <p class="hero-meta">
          <template v-if="showWeddingDetails">
            <span v-if="effectiveWeddingLocation" class="hero-meta-item"
              ><MapPin :size="14" /> {{ effectiveWeddingLocation }}</span
            >
            <span
              v-if="effectiveWeddingLocation && formattedWeddingDate"
              class="hero-dot"
              >·</span
            >
            <span v-if="formattedWeddingDate" class="hero-meta-item">{{
              formattedWeddingDate
            }}</span>
          </template>
          <RouterLink v-else to="/couple/profile" class="hero-meta-set">
            {{ t("overview.setWeddingDate") }}
          </RouterLink>
        </p>
      </div>
      <div
        v-if="showWeddingDetails && daysToGo !== null"
        class="hero-countdown"
      >
        <span class="countdown-number">{{ daysToGo }}</span>
        <span class="countdown-label">{{ t("overview.daysToGo") }}</span>
      </div>
    </div>

    <!-- ── Stat Widgets ────────────────────────────────────────── -->
    <div class="widgets-grid">
      <div class="widget">
        <div class="widget-top">
          <span class="widget-value">{{ tasksDone }}/{{ tasksTotal }}</span>
        </div>
        <p class="widget-label">{{ t("overview.widgets.tasksComplete") }}</p>
        <p v-if="checklistStore.urgent > 0" class="widget-hint urgent-hint">
          <AlertTriangle :size="13" /> {{ checklistStore.urgent }}
          {{ t("overview.widgets.dueSoon") }}
        </p>
        <p v-else class="widget-hint">{{ t("overview.widgets.onTrack") }}</p>
      </div>
      <div class="widget">
        <div class="widget-top">
          <span class="widget-value">{{ formatEur(budgetSpent) }}</span>
        </div>
        <p class="widget-label">{{ t("overview.widgets.budgetSpent") }}</p>
        <p class="widget-hint">
          {{ formatEur(budgetRemaining) }} {{ t("overview.widgets.remaining") }}
        </p>
      </div>
      <div class="widget">
        <div class="widget-top">
          <span class="widget-value">{{ guestsConfirmed }}</span>
        </div>
        <p class="widget-label">{{ t("overview.widgets.guestsConfirmed") }}</p>
        <p class="widget-hint">
          {{ guestsPending }} {{ t("overview.widgets.awaitingRsvp") }}
        </p>
      </div>
      <div class="widget">
        <div class="widget-top">
          <span class="widget-value">{{ vendorsBooked }}</span>
        </div>
        <p class="widget-label">{{ t("overview.widgets.vendorsBooked") }}</p>
        <p class="widget-hint">
          {{ vendorsPendingReply }} {{ t("overview.widgets.pendingReply") }}
        </p>
      </div>
    </div>

    <!-- ── Bottom Grid ─────────────────────────────────────────── -->
    <div class="bottom-grid">
      <!-- Upcoming Tasks -->
      <section class="card tasks-card">
        <div class="card-header">
          <h3>{{ t("overview.upcomingTasks") }}</h3>
          <RouterLink to="/couple/checklist" class="view-all">{{
            t("common.view_all")
          }}</RouterLink>
        </div>
        <div v-if="upcomingTasks.length === 0" class="empty">
          {{ t("overview.noTasks") }}
        </div>
        <ul v-else class="task-list">
          <li
            v-for="task in upcomingTasks"
            :key="task.id"
            class="task-item"
            :class="{ 'task-done': task.done }"
          >
            <span class="task-check">{{ task.done ? "✓" : "○" }}</span>
            <span class="task-label">{{ task.label }}</span>
            <span
              class="task-due"
              :class="{
                'due-urgent': dueDateUrgent(task),
                'due-done': task.done,
              }"
            >
              {{ dueDateLabel(task) }}
            </span>
          </li>
        </ul>
      </section>

      <!-- Wedding Team -->
      <section class="card team-card">
        <div class="card-header">
          <h3>{{ t("overview.myTeam") }}</h3>
          <RouterLink to="/couple/enquiries" class="view-all">{{
            t("common.view_all")
          }}</RouterLink>
        </div>
        <div v-if="teamLeads.length === 0" class="empty">
          {{ t("overview.noVendors") }}
        </div>
        <ul v-else class="team-list">
          <li v-for="lead in teamLeads" :key="lead.id" class="team-item">
            <div class="team-avatar">
              <img
                v-if="lead.vendorProfilePicture"
                :src="lead.vendorProfilePicture"
                class="team-avatar-img"
                alt=""
              />
              <component
                v-else
                :is="categoryIcon(lead.vendorCategory)"
                :size="15"
              />
            </div>
            <div class="team-info">
              <span class="team-name">{{ lead.vendorName ?? "Vendor" }}</span>
              <span class="team-cat">{{
                lead.vendorCategory
                  ? t(`categories.${lead.vendorCategory}`)
                  : ""
              }}</span>
            </div>
            <span class="badge" :class="statusBadgeClass(lead.status)">{{
              statusLabel(lead.status)
            }}</span>
          </li>
        </ul>
      </section>

      <!-- Budget Overview -->
      <section class="card budget-card">
        <div class="card-header">
          <h3>{{ t("overview.budgetOverview") }}</h3>
          <RouterLink to="/couple/budget" class="view-all">{{
            t("common.view_all")
          }}</RouterLink>
        </div>
        <div class="budget-total">
          <span class="budget-spent-num">{{ formatEur(budgetSpent) }}</span>
          <span class="budget-total-label">
            {{ t("overview.spentOf", { total: formatEur(budgetTotal) }) }}</span
          >
        </div>
        <div class="budget-progress-wrap">
          <div class="budget-progress-track">
            <div
              class="budget-progress-fill"
              :style="{
                width:
                  budgetTotal > 0
                    ? `${Math.min(100, Math.round((budgetSpent / budgetTotal) * 100))}%`
                    : '0%',
              }"
            ></div>
          </div>
        </div>
        <ul v-if="budgetByCategory.length > 0" class="budget-cats">
          <li
            v-for="[cat, amount] in budgetByCategory"
            :key="cat"
            class="budget-cat-row"
          >
            <span class="budget-cat-name">{{ categoryLabel(cat) }}</span>
            <div class="budget-cat-bar-wrap">
              <div
                class="budget-cat-bar"
                :style="{ width: budgetBarWidth(amount) }"
              ></div>
            </div>
            <span class="budget-cat-amount">{{ formatEur(amount) }}</span>
          </li>
        </ul>
        <div v-else class="empty">{{ t("overview.noBudgetItems") }}</div>
      </section>

      <!-- Recent Messages -->
      <section class="card messages-card">
        <div class="card-header">
          <h3>{{ t("overview.recentMessages") }}</h3>
          <RouterLink to="/couple/messages" class="view-all">{{
            t("common.view_all")
          }}</RouterLink>
        </div>
        <div v-if="recentThreads.length === 0" class="empty">
          {{ t("overview.noMessages") }}
        </div>
        <ul v-else class="message-list">
          <li
            v-for="thread in recentThreads"
            :key="thread.id"
            class="message-item"
          >
            <MessageCircle class="message-icon" :size="16" />
            <div class="message-body">
              <div class="message-row">
                <span class="message-name">{{ thread.participantName }}</span>
                <span class="message-time">{{
                  timeAgo(thread.lastMessageAt)
                }}</span>
              </div>
              <p class="message-preview">{{ thread.lastMessage }}</p>
            </div>
            <span v-if="thread.unreadCount > 0" class="unread-dot"></span>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* ── Layout ──────────────────────────────────────────────────────────────── */
.overview {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* ── Hero Banner ─────────────────────────────────────────────────────────── */
.hero-banner {
  background: linear-gradient(135deg, #2c2c2c 0%, #1a1a1a 60%, #3a2c1a 100%);
  border-radius: 20px;
  padding: 40px 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  gap: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
}

.hero-welcome {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.55);
  margin: 0 0 6px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  font-weight: 500;
}

.hero-title {
  margin: 0 0 14px;
  line-height: 1.15;
}

.hero-names {
  font-size: 2rem;
  font-weight: 700;
  color: #fff;
}

.hero-subtitle {
  font-size: 2rem;
  font-weight: 300;
  color: rgba(255, 255, 255, 0.65);
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 0.88rem;
  color: rgba(255, 255, 255, 0.6);
}

.hero-dot {
  color: rgba(255, 255, 255, 0.3);
}

.hero-meta-set {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.55);
  text-decoration: none;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.3);
  transition:
    color 0.18s,
    border-color 0.18s;
}
.hero-meta-set:hover {
  color: #d4a843;
  border-color: #d4a843;
}

.hero-countdown {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 20px 28px;
  flex-shrink: 0;
}

.countdown-number {
  font-size: 3rem;
  font-weight: 800;
  color: #d4a843;
  line-height: 1;
}

.countdown-label {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-top: 4px;
}

/* ── Widgets ─────────────────────────────────────────────────────────────── */
.widgets-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 900px) {
  .widgets-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.widget {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 20px 22px;
}

.widget-top {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}

.widget-value {
  font-size: 1.9rem;
  font-weight: 800;
  color: var(--color-text);
  line-height: 1;
}

.widget-label {
  font-size: 0.82rem;
  color: var(--color-muted);
  margin: 0 0 4px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.widget-hint {
  font-size: 0.8rem;
  color: var(--color-muted);
  margin: 0;
}

.urgent-hint {
  color: #d97706;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ── Bottom Grid ─────────────────────────────────────────────────────────── */
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto auto;
  gap: 20px;
}

@media (max-width: 820px) {
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

/* ── Cards ───────────────────────────────────────────────────────────────── */
.card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 22px 24px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.card-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
}

.view-all {
  font-size: 0.8rem;
  color: var(--color-gold);
  text-decoration: none;
  font-weight: 600;
}

.view-all:hover {
  text-decoration: underline;
}

.empty {
  color: var(--color-muted);
  font-size: 0.88rem;
}

/* ── Task List ───────────────────────────────────────────────────────────── */
.task-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.88rem;
}

.task-item:last-child {
  border-bottom: none;
}

.task-check {
  font-size: 0.9rem;
  color: var(--color-gold);
  width: 16px;
  flex-shrink: 0;
}

.task-done .task-check {
  color: #6ca86c;
}

.task-label {
  flex: 1;
  color: var(--color-text);
}

.task-done .task-label {
  color: var(--color-muted);
  text-decoration: line-through;
}

.task-due {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-muted);
  white-space: nowrap;
}

.due-urgent {
  color: #d97706;
}

.due-done {
  color: #6ca86c;
}

/* ── Team List ───────────────────────────────────────────────────────────── */
.team-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.team-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.88rem;
}

.team-item:last-child {
  border-bottom: none;
}

.team-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-bg-2, #f0ebe3);
  color: var(--color-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  position: relative;
  transition:
    transform 0.18s,
    box-shadow 0.18s;
}
.team-avatar:hover {
  transform: scale(3);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  z-index: 9999;
}
.team-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.team-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.team-name {
  font-weight: 600;
  color: var(--color-text);
}

.team-cat {
  font-size: 0.75rem;
  color: var(--color-muted);
  text-transform: capitalize;
}

.badge {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: 20px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.badge-booked {
  background: #e8f5e9;
  color: #2e7d32;
}

.badge-pending {
  background: #fff8e1;
  color: #b8860b;
}

.badge-contacted {
  background: #f0f4ff;
  color: #4a5fa0;
}

.badge-cancelled {
  background: #e2e3e5;
  color: #383d41;
}

/* ── Budget ──────────────────────────────────────────────────────────────── */
.budget-total {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 0.88rem;
  color: var(--color-muted);
}

.budget-spent-num {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text);
}

.budget-total-label {
  font-size: 0.82rem;
  color: var(--color-muted);
}

.budget-progress-wrap {
  margin-bottom: 16px;
}

.budget-progress-track {
  height: 6px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}

.budget-progress-fill {
  height: 100%;
  background: #d4a843;
  border-radius: 4px;
  transition: width 0.4s ease;
}

.budget-cats {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.budget-cat-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.82rem;
}

.budget-cat-name {
  width: 100px;
  color: var(--color-text);
  font-weight: 500;
  flex-shrink: 0;
  text-transform: capitalize;
}

.budget-cat-bar-wrap {
  flex: 1;
  height: 6px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}

.budget-cat-bar {
  height: 100%;
  background: #c9a94a;
  border-radius: 4px;
  transition: width 0.4s ease;
}

.budget-cat-amount {
  width: 44px;
  text-align: right;
  color: var(--color-muted);
  font-weight: 600;
  flex-shrink: 0;
}

/* ── Messages ────────────────────────────────────────────────────────────── */
.message-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border);
  position: relative;
}

.message-item:last-child {
  border-bottom: none;
}

.message-icon {
  color: var(--color-muted);
  margin-top: 2px;
  flex-shrink: 0;
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 2px;
}

.message-name {
  font-weight: 600;
  font-size: 0.88rem;
  color: var(--color-text);
}

.message-time {
  font-size: 0.72rem;
  color: var(--color-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

.message-preview {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d4a843;
  flex-shrink: 0;
  margin-top: 6px;
}
</style>
