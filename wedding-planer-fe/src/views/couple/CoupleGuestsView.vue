<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { useGuestsStore } from "@/stores/guests.store";
import { useCoupleStore } from "@/stores/couple.store";
import { guestsApi } from "@/api/guests.api";
import { coupleApi } from "@/api/couple.api";
import { useToast } from "@/composables/useToast";
import { useDebounce } from "@/composables/useDebounce";
import { useConfirm } from "@/composables/useConfirm";
import type {
  Guest,
  GuestRequest,
  GuestSide,
  GuestGroup,
  RsvpStatus,
} from "@/types/couple.types";

const { t } = useI18n();
const store = useGuestsStore();
const coupleStore = useCoupleStore();
const toast = useToast();

// -- Tabs ------------------------------------------------------------------
const activeTab = ref<"list" | "tables">("list");

// -- Filters ---------------------------------------------------------------
const search = ref("");
const debouncedSearch = useDebounce(() => search.value, 300);
const filterRsvp = ref<"" | RsvpStatus>("");
const filterSide = ref<"" | GuestSide>("");
const filterGroup = ref<"" | GuestGroup>("");
const filterDiet = ref("");

const filtered = computed(() =>
  store.guests.filter((g) => {
    const q = debouncedSearch.value.toLowerCase();
    const matchesSearch =
      !q ||
      g.fullName.toLowerCase().includes(q) ||
      (g.email ?? "").toLowerCase().includes(q) ||
      (g.tableAssignment ?? "").toLowerCase().includes(q);
    const matchesRsvp = !filterRsvp.value || g.rsvpStatus === filterRsvp.value;
    const matchesSide = !filterSide.value || g.side === filterSide.value;
    const matchesGroup =
      !filterGroup.value || g.guestGroup === filterGroup.value;
    const matchesDiet = !filterDiet.value || g.dietary === filterDiet.value;
    return (
      matchesSearch && matchesRsvp && matchesSide && matchesGroup && matchesDiet
    );
  }),
);

// -- Sorting ---------------------------------------------------------------
const sortKey = ref<keyof Guest>("firstName");
const sortAsc = ref(true);

function sort(key: keyof Guest) {
  if (sortKey.value === key) sortAsc.value = !sortAsc.value;
  else {
    sortKey.value = key;
    sortAsc.value = true;
  }
}

const sorted = computed(() =>
  [...filtered.value].sort((a, b) => {
    const va = String(a[sortKey.value] ?? "").toLowerCase();
    const vb = String(b[sortKey.value] ?? "").toLowerCase();
    return sortAsc.value ? va.localeCompare(vb) : vb.localeCompare(va);
  }),
);

function sortHeader(key: keyof Guest) {
  return sortKey.value === key ? (sortAsc.value ? " \u25b2" : " \u25bc") : "";
}

// -- Smart Alerts ----------------------------------------------------------
const alerts = computed(() => {
  const s = store.stats;
  if (!s || s.total === 0) return [];
  const msgs: { type: "error" | "warn" | "ok"; text: string }[] = [];
  if (s.overCapacity)
    msgs.push({
      type: "error",
      text: t("guests.alerts.overCapacity", {
        confirmed: s.confirmed,
        estimated: s.estimatedCapacity,
      }),
    });
  if (!s.overCapacity && s.pending > 0 && s.pending / s.total > 0.2)
    msgs.push({
      type: "warn",
      text: t("guests.alerts.notResponded", {
        pending: s.pending,
        percent: Math.round((s.pending / s.total) * 100),
      }),
    });
  if (s.confirmed > 0 && s.pending === 0)
    msgs.push({
      type: "ok",
      text: t("guests.alerts.allResponded"),
    });
  const dietaryCount = Object.entries(s.byDietary)
    .filter(([k, v]) => k !== "NONE" && v > 0)
    .reduce((acc, [, v]) => acc + v, 0);
  if (dietaryCount > 0)
    msgs.push({
      type: "warn",
      text: t("guests.alerts.dietaryReminder", { count: dietaryCount }),
    });
  return msgs;
});

// -- RSVP quick-toggle -----------------------------------------------------
const rsvpCycle: RsvpStatus[] = ["PENDING", "CONFIRMED", "DECLINED"];

async function cycleRsvp(guest: Guest) {
  const nextIdx = (rsvpCycle.indexOf(guest.rsvpStatus) + 1) % rsvpCycle.length;
  await store.updateGuest(guest.id, {
    ...buildRequest(guest),
    rsvpStatus: rsvpCycle[nextIdx],
  });
}

function buildRequest(g: Guest): GuestRequest {
  return {
    firstName: g.firstName,
    lastName: g.lastName,
    email: g.email,
    phone: g.phone,
    side: g.side,
    guestGroup: g.guestGroup,
    rsvpStatus: g.rsvpStatus,
    plusOne: g.plusOne,
    plusOneName: g.plusOneName,
    dietary: g.dietary,
    dietaryNotes: g.dietaryNotes,
    tableAssignment: g.tableAssignment,
    notes: g.notes,
    isChildGuest: g.isChildGuest,
  };
}

// -- Add / Edit modal ------------------------------------------------------
const showModal = ref(false);
const editTarget = ref<Guest | null>(null);
const form = ref<GuestRequest>(emptyForm());
const saving = ref(false);

function emptyForm(): GuestRequest {
  return {
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    side: "BOTH",
    guestGroup: "OTHER",
    rsvpStatus: "PENDING",
    plusOne: false,
    plusOneName: "",
    dietary: "NONE",
    dietaryNotes: "",
    tableAssignment: "",
    notes: "",
    isChildGuest: false,
  };
}

function openAdd() {
  editTarget.value = null;
  form.value = emptyForm();
  showModal.value = true;
}

function openEdit(g: Guest) {
  editTarget.value = g;
  form.value = buildRequest(g);
  showModal.value = true;
}

async function saveGuest() {
  if (!form.value.firstName?.trim()) return;
  saving.value = true;
  try {
    if (editTarget.value)
      await store.updateGuest(editTarget.value.id, form.value);
    else await store.addGuest(form.value);
    showModal.value = false;
  } finally {
    saving.value = false;
  }
}

async function confirmDelete(g: Guest) {
  const companion = store.guests.find((c) => c.invitedById === g.id);
  const ok = await useConfirm().ask(
    companion
      ? `Remove ${g.fullName} and their +1 (${companion.fullName || "+1"}) from the guest list?`
      : `Remove ${g.fullName} from the guest list?`,
    { title: "Remove Guest", confirmText: "Remove" },
  );
  if (ok) {
    if (companion) await store.removeGuest(companion.id);
    await store.removeGuest(g.id);
  }
}

// -- Inline table assignment -----------------------------------------------
async function setTable(g: Guest, table: string) {
  await store.updateGuest(g.id, { ...buildRequest(g), tableAssignment: table });
  const companion = store.guests.find((c) => c.invitedById === g.id);
  if (companion) {
    await store.updateGuest(companion.id, {
      ...buildRequest(companion),
      tableAssignment: table,
    });
  }
}

// -- CSV Import modal ------------------------------------------------------
const showImport = ref(false);
const csvText = ref("");
const importCount = ref<number | null>(null);
const importing = ref(false);

async function runImport() {
  if (!csvText.value.trim()) return;
  importing.value = true;
  try {
    importCount.value = await store.importCsv(csvText.value);
    csvText.value = "";
    setTimeout(() => {
      showImport.value = false;
      importCount.value = null;
    }, 1500);
  } finally {
    importing.value = false;
  }
}

// -- Lifecycle -------------------------------------------------------------
const websiteSubdomain = ref<string | null>(null);
const websitePublished = ref(false);

onMounted(async () => {
  await store.fetchGuests();
  if (!coupleStore.profile) await coupleStore.fetchProfile();
  try {
    const ws = await coupleApi.getWebsite();
    websiteSubdomain.value = ws.data?.subdomain ?? null;
    websitePublished.value = ws.data?.published ?? false;
  } catch {
    // website not set up yet
  }
});

// -- CSV export ------------------------------------------------------------
function exportCsv() {
  guestsApi.exportCsv().then((res) => {
    const url = URL.createObjectURL(
      new Blob([res.data as unknown as BlobPart]),
    );
    const a = document.createElement("a");
    a.href = url;
    a.download = "guests.csv";
    a.click();
    URL.revokeObjectURL(url);
  });
}

// -- Labels ----------------------------------------------------------------
const rsvpLabel = computed<Record<string, string>>(() => ({
  CONFIRMED: t("guests.statuses.CONFIRMED"),
  DECLINED: t("guests.statuses.DECLINED"),
  PENDING: t("guests.statuses.PENDING"),
}));
const dietaryLabel = computed<Record<string, string>>(() => ({
  NONE: t("guests.meals.NONE"),
  VEGAN: t("guests.meals.VEGAN"),
  VEGETARIAN: t("guests.meals.VEGETARIAN"),
  GLUTEN_FREE: t("guests.meals.GLUTEN_FREE"),
  HALAL: t("guests.meals.HALAL"),
  KOSHER: t("guests.meals.KOSHER"),
  OTHER: t("guests.meals.OTHER"),
}));
const sideLabel = computed<Record<string, string>>(() => ({
  BOTH: t("guests.sideOptions.both"),
  BRIDE: t("guests.sideOptions.bride"),
  GROOM: t("guests.sideOptions.groom"),
}));
const groupLabel = computed<Record<string, string>>(() => ({
  FAMILY: t("guests.groupOptions.family"),
  FRIENDS: t("guests.groupOptions.friends"),
  COLLEAGUES: t("guests.groupOptions.colleagues"),
  OTHER: t("guests.groupOptions.other"),
}));

// -- Table Planner ---------------------------------------------------------
const tableNames = computed(() => {
  const s = new Set<string>();
  store.guests.forEach((g) => {
    if (g.tableAssignment) s.add(g.tableAssignment);
  });
  return [...s].sort();
});

function guestsAtTable(table: string) {
  return store.guests.filter((g) => g.tableAssignment === table);
}

const newTableName = ref("");

// -- Invite link -----------------------------------------------------------
const copyingLink = ref(new Set<string>());
const copiedLink = ref(new Set<string>());

async function copyInviteLink(g: Guest) {
  const subdomain = websiteSubdomain.value;
  if (!subdomain) {
    toast.warn(t("guests.invite.noWebsiteWarning"));
    return;
  }
  copyingLink.value = new Set([...copyingLink.value, g.id]);
  try {
    const res = await guestsApi.generateInviteLink(g.id);
    const token = res.data.token;
    const base = window.location.origin;
    const url = `${base}/w/${subdomain}?guest=${token}`;

    await navigator.clipboard.writeText(url);
    await store.fetchGuests();
    copiedLink.value = new Set([...copiedLink.value, g.id]);
    setTimeout(() => {
      const next = new Set(copiedLink.value);
      next.delete(g.id);
      copiedLink.value = next;
    }, 2000);
  } finally {
    const next = new Set(copyingLink.value);
    next.delete(g.id);
    copyingLink.value = next;
  }
}
</script>

<template>
  <div class="guests-view">
    <!-- Page header -->
    <div class="page-header">
      <div>
        <h2>{{ t("guests.title") }}</h2>
        <p class="page-sub">{{ t("guests.subtitle") }}</p>
      </div>
      <div class="header-actions">
        <button class="btn-outline" @click="showImport = true">
          &#8679; {{ t("guests.importCsv") }}
        </button>
        <button class="btn-outline" @click="exportCsv">
          &#8681; {{ t("guests.exportCsv") }}
        </button>
        <button class="btn-primary" @click="openAdd">
          + {{ t("guests.addGuest") }}
        </button>
      </div>
    </div>

    <!-- Stats bar -->
    <div v-if="store.stats" class="stats-bar">
      <div class="stat">
        <span class="stat-num">{{ store.stats.total }}</span>
        <span class="stat-label">{{ t("guests.total") }}</span>
      </div>
      <div class="stat confirmed">
        <span class="stat-num">{{ store.stats.confirmed }}</span>
        <span class="stat-label">{{ t("guests.confirmed") }}</span>
      </div>
      <div class="stat declined">
        <span class="stat-num">{{ store.stats.declined }}</span>
        <span class="stat-label">{{ t("guests.declined") }}</span>
      </div>
      <div class="stat pending">
        <span class="stat-num">{{ store.stats.pending }}</span>
        <span class="stat-label">{{ t("guests.pending") }}</span>
      </div>
      <div class="progress-wrap" v-if="store.stats.estimatedCapacity > 0">
        <div class="progress-label">
          {{ t("guests.capacity") }}: {{ store.stats.confirmed }} /
          {{ store.stats.estimatedCapacity }}
        </div>
        <div class="progress-bar">
          <div
            class="progress-fill"
            :class="{ over: store.stats.overCapacity }"
            :style="{
              width:
                Math.min(
                  100,
                  (store.stats.confirmed / store.stats.estimatedCapacity) * 100,
                ) + '%',
            }"
          />
        </div>
      </div>
    </div>

    <!-- Smart alerts -->
    <div v-for="a in alerts" :key="a.text" class="alert" :class="a.type">
      {{ a.text }}
    </div>

    <!-- Tabs -->
    <div class="tabs">
      <button
        :class="{ active: activeTab === 'list' }"
        @click="activeTab = 'list'"
      >
        {{ t("guests.title") }}
      </button>
      <button
        :class="{ active: activeTab === 'tables' }"
        @click="activeTab = 'tables'"
      >
        {{ t("guests.tablePlanner") }}
      </button>
    </div>

    <!-- ============ GUEST LIST TAB ============ -->
    <template v-if="activeTab === 'list'">
      <!-- Filter bar -->
      <div class="filter-bar">
        <input
          v-model="search"
          :placeholder="t('guests.searchPlaceholder')"
          class="search-input"
        />

        <select v-model="filterRsvp">
          <option value="">{{ t("guests.allRsvp") }}</option>
          <option value="CONFIRMED">
            {{ t("guests.statuses.CONFIRMED") }}
          </option>
          <option value="DECLINED">{{ t("guests.statuses.DECLINED") }}</option>
          <option value="PENDING">{{ t("guests.statuses.PENDING") }}</option>
        </select>

        <select v-model="filterSide">
          <option value="">{{ t("guests.allSides") }}</option>
          <option value="BRIDE">{{ t("guests.sideOptions.bride") }}</option>
          <option value="GROOM">{{ t("guests.sideOptions.groom") }}</option>
          <option value="BOTH">{{ t("guests.sideOptions.both") }}</option>
        </select>

        <select v-model="filterGroup">
          <option value="">{{ t("guests.allGroups") }}</option>
          <option value="FAMILY">{{ t("guests.groupOptions.family") }}</option>
          <option value="FRIENDS">
            {{ t("guests.groupOptions.friends") }}
          </option>
          <option value="COLLEAGUES">
            {{ t("guests.groupOptions.colleagues") }}
          </option>
          <option value="OTHER">{{ t("guests.groupOptions.other") }}</option>
        </select>

        <select v-model="filterDiet">
          <option value="">{{ t("guests.allDietary") }}</option>
          <option value="NONE">{{ t("guests.meals.NONE") }}</option>
          <option value="VEGAN">{{ t("guests.meals.VEGAN") }}</option>
          <option value="VEGETARIAN">{{ t("guests.meals.VEGETARIAN") }}</option>
          <option value="GLUTEN_FREE">
            {{ t("guests.meals.GLUTEN_FREE") }}
          </option>
          <option value="HALAL">{{ t("guests.meals.HALAL") }}</option>
          <option value="KOSHER">{{ t("guests.meals.KOSHER") }}</option>
          <option value="OTHER">{{ t("guests.meals.OTHER") }}</option>
        </select>
      </div>

      <!-- Table -->
      <div v-if="store.loading" class="loading">{{ t("common.loading") }}</div>
      <div v-else-if="sorted.length === 0" class="empty">
        {{ t("guests.noMatch") }}
      </div>
      <div v-else class="table-wrap">
        <table class="guest-table">
          <thead>
            <tr>
              <th @click="sort('firstName')" class="sortable">
                {{ t("guests.name") }}{{ sortHeader("firstName") }}
              </th>
              <th @click="sort('rsvpStatus')" class="sortable">
                {{ t("guests.rsvpStatus") }}{{ sortHeader("rsvpStatus") }}
              </th>
              <th>{{ t("guests.invite") }}</th>
              <th @click="sort('side')" class="sortable">
                {{ t("guests.side") }}{{ sortHeader("side") }}
              </th>
              <th @click="sort('guestGroup')" class="sortable">
                {{ t("guests.group") }}{{ sortHeader("guestGroup") }}
              </th>
              <th @click="sort('dietary')" class="sortable">
                {{ t("guests.dietary") }}{{ sortHeader("dietary") }}
              </th>
              <th @click="sort('tableAssignment')" class="sortable">
                {{ t("guests.table") }}{{ sortHeader("tableAssignment") }}
              </th>
              <th>{{ t("guests.plusOne") }}</th>
              <th>{{ t("guests.actions") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="g in sorted"
              :key="g.id"
              :class="{ child: g.isChildGuest, companion: !!g.invitedById }"
            >
              <td>
                <div class="guest-name">
                  {{ g.fullName }}
                  <span v-if="g.isChildGuest" class="tag child-tag">{{
                    t("guests.childTag")
                  }}</span>
                </div>
                <div class="guest-sub" v-if="g.email">{{ g.email }}</div>
                <div class="guest-sub" v-if="g.phone">{{ g.phone }}</div>
              </td>
              <td>
                <span
                  v-if="(g.inviteStatus ?? 'NOT_INVITED') === 'NOT_INVITED'"
                  class="muted rsvp-not-invited"
                  :title="t('guests.invite.notSentTitle')"
                  >—</span
                >
                <span
                  v-else-if="g.inviteStatus === 'LINK_SENT'"
                  class="rsvp-badge link-sent"
                  :title="t('guests.invite.sentTitle')"
                  >{{ t("guests.invite.invited") }}</span
                >
                <button
                  v-else
                  class="rsvp-badge"
                  :class="g.rsvpStatus.toLowerCase()"
                  @click="cycleRsvp(g)"
                  title="Click to cycle RSVP status"
                >
                  {{ rsvpLabel[g.rsvpStatus] }}
                </button>
              </td>
              <td class="invite-cell">
                <RouterLink
                  v-if="!websiteSubdomain"
                  to="/couple/website"
                  class="setup-website-link"
                  :title="t('guests.setupWebsiteTooltip')"
                >
                  {{ t("guests.setupWebsite") }}
                </RouterLink>
                <button
                  v-else-if="!websitePublished"
                  class="icon-btn copy-link-btn unpublished"
                  disabled
                  :title="t('guests.invite.notPublishedTooltip')"
                >
                  🔒 {{ t("guests.invite.notPublished") }}
                </button>
                <button
                  v-else
                  class="icon-btn copy-link-btn"
                  :disabled="copyingLink.has(g.id)"
                  @click="copyInviteLink(g)"
                  :title="
                    copiedLink.has(g.id)
                      ? t('guests.invite.copied') + '!'
                      : g.inviteStatus !== 'NOT_INVITED'
                        ? t('guests.invite.resendLink')
                        : t('guests.invite.copyLink')
                  "
                >
                  {{
                    copiedLink.has(g.id)
                      ? "✓ " + t("guests.invite.copied")
                      : g.inviteStatus !== "NOT_INVITED"
                        ? "🔗 " + t("guests.invite.resendLink")
                        : "🔗 " + t("guests.invite.copyLink")
                  }}
                </button>
              </td>
              <td>
                <span class="tag">{{ sideLabel[g.side] }}</span>
              </td>
              <td>
                <span class="tag">{{ groupLabel[g.guestGroup] }}</span>
              </td>
              <td>
                <span v-if="g.dietary !== 'NONE'" class="tag dietary-tag">{{
                  dietaryLabel[g.dietary]
                }}</span>
                <span v-else class="muted">&mdash;</span>
                <div class="guest-sub" v-if="g.dietaryNotes">
                  {{ g.dietaryNotes }}
                </div>
              </td>
              <td>
                <input
                  class="table-input"
                  :value="g.tableAssignment"
                  placeholder="--"
                  @blur="setTable(g, ($event.target as HTMLInputElement).value)"
                  @keydown.enter="($event.target as HTMLInputElement).blur()"
                />
              </td>
              <td>
                <span v-if="g.plusOne" class="tag"
                  >+1
                  <span v-if="g.plusOneName"> {{ g.plusOneName }}</span>
                </span>
                <span v-else class="muted">&mdash;</span>
              </td>
              <td class="actions">
                <button class="icon-btn" title="Edit" @click="openEdit(g)">
                  {{ t("common.edit") }}
                </button>
                <button
                  class="icon-btn del"
                  title="Delete"
                  @click="confirmDelete(g)"
                >
                  {{ t("common.delete") }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- ============ TABLE PLANNER TAB ============ -->
    <template v-if="activeTab === 'tables'">
      <div class="table-planner">
        <div class="planner-add">
          <input
            v-model="newTableName"
            :placeholder="t('guests.newTablePlaceholder')"
            class="fi"
          />
        </div>

        <div
          v-if="
            tableNames.length === 0 &&
            store.guests.filter((g) => !g.tableAssignment).length === 0
          "
          class="empty"
        >
          {{ t("guests.noTablesAssigned") }}
        </div>

        <div v-for="table in tableNames" :key="table" class="table-card">
          <h4 class="table-card-title">
            {{ table }}
            <span class="muted">({{ guestsAtTable(table).length }})</span>
          </h4>
          <div
            v-for="g in guestsAtTable(table)"
            :key="g.id"
            class="table-guest"
          >
            <span class="rsvp-dot" :class="g.rsvpStatus.toLowerCase()" />
            {{ g.fullName }}
            <span v-if="g.dietary !== 'NONE'" class="tag dietary-tag sm">{{
              dietaryLabel[g.dietary]
            }}</span>
          </div>
        </div>

        <div
          v-if="store.guests.filter((g) => !g.tableAssignment).length > 0"
          class="table-card unassigned"
        >
          <h4 class="table-card-title">
            {{ t("guests.unassigned") }}
            <span class="muted"
              >({{
                store.guests.filter((g) => !g.tableAssignment).length
              }})</span
            >
          </h4>
          <div
            v-for="g in store.guests.filter((g) => !g.tableAssignment)"
            :key="g.id"
            class="table-guest"
          >
            <span class="rsvp-dot" :class="g.rsvpStatus.toLowerCase()" />
            {{ g.fullName }}
          </div>
        </div>
      </div>
    </template>

    <!-- ============ ADD / EDIT MODAL ============ -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <h3>{{ editTarget ? t("guests.editGuest") : t("guests.addGuest") }}</h3>

        <div class="modal-grid">
          <label>
            {{ t("guests.firstName") }} *
            <input
              v-model="form.firstName"
              class="fi"
              :placeholder="t('guests.firstName')"
            />
          </label>
          <label>
            {{ t("guests.lastName") }}
            <input
              v-model="form.lastName"
              class="fi"
              :placeholder="t('guests.lastName')"
            />
          </label>
          <label>
            {{ t("guests.email") }}
            <input
              v-model="form.email"
              class="fi"
              type="email"
              placeholder="email@example.com"
            />
          </label>
          <label>
            {{ t("guests.phone") }}
            <input
              v-model="form.phone"
              class="fi"
              placeholder="+40 700 000 000"
            />
          </label>

          <label>
            {{ t("guests.side") }}
            <select v-model="form.side" class="fi">
              <option value="BOTH">{{ t("guests.sideOptions.both") }}</option>
              <option value="BRIDE">{{ t("guests.sideOptions.bride") }}</option>
              <option value="GROOM">{{ t("guests.sideOptions.groom") }}</option>
            </select>
          </label>
          <label>
            {{ t("guests.group") }}
            <select v-model="form.guestGroup" class="fi">
              <option value="FAMILY">
                {{ t("guests.groupOptions.family") }}
              </option>
              <option value="FRIENDS">
                {{ t("guests.groupOptions.friends") }}
              </option>
              <option value="COLLEAGUES">
                {{ t("guests.groupOptions.colleagues") }}
              </option>
              <option value="OTHER">
                {{ t("guests.groupOptions.other") }}
              </option>
            </select>
          </label>

          <label>
            {{ t("guests.rsvpStatus") }}
            <select v-model="form.rsvpStatus" class="fi">
              <option value="PENDING">
                {{ t("guests.statuses.PENDING") }}
              </option>
              <option value="CONFIRMED">
                {{ t("guests.statuses.CONFIRMED") }}
              </option>
              <option value="DECLINED">
                {{ t("guests.statuses.DECLINED") }}
              </option>
            </select>
          </label>
          <label>
            {{ t("guests.dietary") }}
            <select v-model="form.dietary" class="fi">
              <option value="NONE">{{ t("guests.meals.NONE") }}</option>
              <option value="VEGAN">{{ t("guests.meals.VEGAN") }}</option>
              <option value="VEGETARIAN">
                {{ t("guests.meals.VEGETARIAN") }}
              </option>
              <option value="GLUTEN_FREE">
                {{ t("guests.meals.GLUTEN_FREE") }}
              </option>
              <option value="HALAL">{{ t("guests.meals.HALAL") }}</option>
              <option value="KOSHER">{{ t("guests.meals.KOSHER") }}</option>
              <option value="OTHER">{{ t("guests.meals.OTHER") }}</option>
            </select>
          </label>

          <label style="grid-column: span 2">
            {{ t("guests.dietaryNotes") }}
            <input
              v-model="form.dietaryNotes"
              class="fi"
              placeholder="Any specific notes..."
            />
          </label>

          <label>
            {{ t("guests.tableAssignment") }}
            <input
              v-model="form.tableAssignment"
              class="fi"
              placeholder="Table 1"
            />
          </label>
          <label style="grid-column: span 2">
            {{ t("guests.notes") }}
            <textarea
              v-model="form.notes"
              class="fi"
              rows="2"
              placeholder="Any additional notes..."
            />
          </label>

          <label class="checkbox-label">
            <input type="checkbox" v-model="form.plusOne" />
            {{ t("guests.bringingPlusOne") }}
          </label>
          <label v-if="form.plusOne">
            {{ t("guests.plusOneName") }}
            <input
              v-model="form.plusOneName"
              class="fi"
              placeholder="Guest's +1 name"
            />
          </label>

          <label class="checkbox-label">
            <input type="checkbox" v-model="form.isChildGuest" />
            {{ t("guests.childGuest") }}
          </label>
        </div>

        <div class="modal-footer">
          <button class="btn-outline" @click="showModal = false">
            {{ t("common.cancel") }}
          </button>
          <button
            class="btn-primary"
            :disabled="saving || !form.firstName?.trim()"
            @click="saveGuest"
          >
            {{
              saving
                ? t("guests.saving")
                : editTarget
                  ? t("guests.update")
                  : t("guests.addGuest")
            }}
          </button>
        </div>
      </div>
    </div>

    <!-- ============ CSV IMPORT MODAL ============ -->
    <div
      v-if="showImport"
      class="modal-overlay"
      @click.self="showImport = false"
    >
      <div class="modal">
        <h3>{{ t("guests.importTitle") }}</h3>
        <p class="import-hint">
          {{ t("guests.importHint") }}
        </p>
        <textarea
          v-model="csvText"
          class="fi csv-textarea"
          rows="10"
          placeholder="John,Doe,john@example.com,+40700000000"
        />
        <div v-if="importCount !== null" class="import-success">
          &#10003; {{ t("guests.importSuccess", { count: importCount }) }}
        </div>
        <div class="modal-footer">
          <button
            class="btn-outline"
            @click="
              showImport = false;
              importCount = null;
            "
          >
            {{ t("common.close") }}
          </button>
          <button
            class="btn-primary"
            :disabled="importing || !csvText.trim()"
            @click="runImport"
          >
            {{ importing ? t("guests.importing") : t("guests.importCsv") }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.guests-view {
  padding: 4px 0 40px;
}

/* -- Header --------------------------------------------------------------- */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0 0 4px;
  font-size: 1.4rem;
}
.page-sub {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-muted);
}
.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* -- Buttons -------------------------------------------------------------- */
.btn-primary {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 700;
  cursor: pointer;
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-outline {
  background: none;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  padding: 7px 14px;
  font-weight: 600;
  cursor: pointer;
  font-size: 0.85rem;
}
.btn-outline:hover {
  border-color: var(--color-gold);
  color: var(--color-gold);
}

/* -- Stats bar ------------------------------------------------------------ */
.stats-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 16px;
}
.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 52px;
}
.stat-num {
  font-size: 1.4rem;
  font-weight: 800;
  line-height: 1;
}
.stat-label {
  font-size: 0.72rem;
  color: var(--color-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.stat.confirmed .stat-num {
  color: var(--color-green, #27ae60);
}
.stat.declined .stat-num {
  color: var(--color-error);
}
.stat.pending .stat-num {
  color: var(--color-gold);
}
.progress-wrap {
  flex: 1;
  min-width: 160px;
}
.progress-label {
  font-size: 0.8rem;
  color: var(--color-muted);
  margin-bottom: 4px;
}
.progress-bar {
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--color-gold);
  border-radius: 4px;
  transition: width 0.4s;
}
.progress-fill.over {
  background: var(--color-error);
}

/* -- Alerts --------------------------------------------------------------- */
.alert {
  border-radius: 8px;
  padding: 10px 16px;
  margin-bottom: 10px;
  font-size: 0.88rem;
  font-weight: 500;
}
.alert.error {
  background: var(--color-error-light);
  color: var(--color-error);
  border: 1px solid var(--color-error);
}
.alert.warn {
  background: var(--color-amber-light);
  color: var(--color-amber);
  border: 1px solid var(--color-amber);
}
.alert.ok {
  background: var(--color-green-light);
  color: var(--color-green);
  border: 1px solid var(--color-green);
}

/* -- Tabs ----------------------------------------------------------------- */
.tabs {
  display: flex;
  border-bottom: 2px solid var(--color-border);
  margin-bottom: 20px;
}
.tabs button {
  background: none;
  border: none;
  padding: 10px 20px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-muted);
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
}
.tabs button.active {
  color: var(--color-gold);
  border-bottom-color: var(--color-gold);
}

/* -- Filter bar ----------------------------------------------------------- */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
  align-items: center;
}
.search-input {
  padding: 7px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.88rem;
  min-width: 200px;
  flex: 1;
}
.filter-bar select {
  padding: 7px 10px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.85rem;
  background: var(--color-white, #fff);
  cursor: pointer;
}

/* -- Guest table ---------------------------------------------------------- */
.table-wrap {
  overflow-x: auto;
}
.guest-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}
.guest-table th {
  text-align: left;
  padding: 8px 12px;
  border-bottom: 2px solid var(--color-border);
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
  color: var(--color-muted);
}
.guest-table th.sortable {
  cursor: pointer;
  user-select: none;
}
.guest-table th.sortable:hover {
  color: var(--color-gold);
}
.guest-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
}
.guest-table tr.child td {
  background: var(--color-surface);
}
.guest-name {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}
.guest-sub {
  font-size: 0.78rem;
  color: var(--color-muted);
  margin-top: 1px;
}
.muted {
  color: var(--color-muted);
}

/* -- RSVP badge ----------------------------------------------------------- */
.rsvp-badge {
  border-radius: 12px;
  padding: 3px 10px;
  font-size: 0.76rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  white-space: nowrap;
}
.rsvp-badge.confirmed {
  background: var(--chip-green-bg, #eafaf1);
  color: var(--color-green, #27ae60);
}
.rsvp-badge.declined {
  background: var(--chip-red-bg, #fde8e8);
  color: var(--color-error);
}
.rsvp-badge.pending {
  background: var(--chip-amber-bg, #fef9e7);
  color: var(--color-gold);
}
.rsvp-badge.link-sent {
  background: #eaf4fb;
  color: #2e86c1;
  cursor: default;
}
.rsvp-badge:hover {
  filter: brightness(0.93);
}

/* -- Invite cell ---------------------------------------------------------- */
.invite-cell {
  vertical-align: middle;
  white-space: nowrap;
}
.invite-badge {
  display: inline-block;
  border-radius: 10px;
  padding: 2px 9px;
  font-size: 0.72rem;
  font-weight: 700;
}
.invite-badge.not-invited {
  background: var(--color-surface);
  color: var(--color-muted, #999);
  border: 1px solid var(--color-border);
}
.invite-badge.link-sent {
  background: #eaf4fb;
  color: #2e86c1;
}
.invite-badge.pending {
  background: var(--chip-amber-bg, #fef9e7);
  color: var(--color-gold);
}
.invite-badge.accepted {
  background: var(--chip-green-bg, #eafaf1);
  color: var(--color-green, #27ae60);
}
.invite-badge.declined {
  background: var(--chip-red-bg, #fde8e8);
  color: var(--color-error);
}
.copy-link-btn {
  font-size: 0.72rem;
  padding: 2px 8px;
  cursor: pointer;
}
.copy-link-btn.unpublished {
  opacity: 0.55;
  cursor: not-allowed;
  border-color: var(--color-amber, #e67e22);
  color: var(--color-amber, #e67e22);
}
.setup-website-link {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--color-amber, #e67e22);
  text-decoration: none;
  border: 1px solid var(--color-amber, #e67e22);
  border-radius: 4px;
  padding: 2px 8px;
  white-space: nowrap;
  display: inline-block;
}
.setup-website-link:hover {
  background: var(--color-amber-light, #fef9e7);
}
.rsvp-not-invited {
  font-size: 0.88rem;
  padding: 3px 10px;
  display: inline-block;
}

/* -- Tags ----------------------------------------------------------------- */
.tag {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 600;
  border-radius: 10px;
  padding: 2px 8px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  white-space: nowrap;
}
.tag.child-tag {
  background: #eaf4fb;
  color: #2e86c1;
  border-color: #aed6f1;
}
.tag.dietary-tag {
  background: #fdf2e9;
  color: #d35400;
  border-color: #fad7a0;
}
.tag.dietary-tag.sm {
  font-size: 0.68rem;
  padding: 1px 6px;
}

/* -- Inline table input --------------------------------------------------- */
.table-input {
  width: 80px;
  border: 1px dashed var(--color-border);
  border-radius: 6px;
  padding: 3px 7px;
  font-size: 0.82rem;
  background: transparent;
}
.table-input:focus {
  border-color: var(--color-gold);
  outline: none;
}

/* -- Row actions ---------------------------------------------------------- */
.actions {
  white-space: nowrap;
}
.icon-btn {
  background: none;
  border: 1px solid var(--color-border);
  cursor: pointer;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  opacity: 0.7;
  margin-right: 4px;
}
.icon-btn:hover {
  opacity: 1;
  background: var(--color-surface);
}
.icon-btn.del:hover {
  border-color: var(--color-error);
  color: var(--color-error);
}

/* -- Table planner -------------------------------------------------------- */
.table-planner {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.table-card {
  min-width: 200px;
  max-width: 280px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 14px 16px;
}
.table-card.unassigned {
  border-style: dashed;
  opacity: 0.8;
}
.table-card-title {
  margin: 0 0 10px;
  font-size: 0.95rem;
  font-weight: 700;
}
.table-guest {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  font-size: 0.85rem;
  border-bottom: 1px solid var(--color-border);
}
.table-guest:last-child {
  border-bottom: none;
}
.rsvp-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.rsvp-dot.confirmed {
  background: var(--color-green, #27ae60);
}
.rsvp-dot.declined {
  background: var(--color-error);
}
.rsvp-dot.pending {
  background: var(--color-gold);
}
.planner-add {
  width: 100%;
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}

/* -- Modal ---------------------------------------------------------------- */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.modal {
  background: var(--color-white, #fff);
  border-radius: 14px;
  padding: 24px 28px;
  width: 100%;
  max-width: 620px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}
.modal h3 {
  margin: 0 0 18px;
  font-size: 1.2rem;
}
.modal-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.modal-grid label {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text);
}
.modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}
.checkbox-label {
  flex-direction: row !important;
  align-items: center;
  gap: 8px !important;
  font-size: 0.88rem !important;
}

/* -- Form inputs ---------------------------------------------------------- */
.fi {
  padding: 8px 10px;
  border: 1.5px solid var(--color-border);
  border-radius: 7px;
  font-size: 0.88rem;
  outline: none;
  width: 100%;
  box-sizing: border-box;
  background: var(--color-white, #fff);
}
.fi:focus {
  border-color: var(--color-gold);
}
textarea.fi {
  resize: vertical;
}
.csv-textarea {
  font-family: monospace;
  font-size: 0.82rem;
}

/* -- Import --------------------------------------------------------------- */
.import-hint {
  font-size: 0.83rem;
  color: var(--color-muted);
  margin-bottom: 12px;
}
.import-hint code {
  background: var(--color-surface);
  padding: 1px 5px;
  border-radius: 4px;
}
.import-success {
  color: var(--color-green, #27ae60);
  font-weight: 600;
  margin-top: 10px;
}

/* -- Misc ----------------------------------------------------------------- */
.loading {
  text-align: center;
  color: var(--color-muted);
  padding: 40px;
}
.empty {
  text-align: center;
  color: var(--color-muted);
  padding: 40px;
}
</style>
