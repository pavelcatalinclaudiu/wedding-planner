<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { coupleApi } from "@/api/couple.api";
import type { SeatingLayout, Guest } from "@/types/couple.types";
import { useGuestsStore } from "@/stores/guests.store";

const { t } = useI18n();
const guestsStore = useGuestsStore();
const layout = ref<SeatingLayout | null>(null);
const loading = ref(false);

async function fetchLayout() {
  loading.value = true;
  try {
    layout.value = await coupleApi.getSeatingLayout();
  } finally {
    loading.value = false;
  }
}

async function assignGuest(tableId: string, guestId: string) {
  await coupleApi.assignGuestToTable(tableId, guestId);
  await fetchLayout();
}

onMounted(async () => {
  await Promise.all([guestsStore.fetchGuests(), fetchLayout()]);
});
</script>

<template>
  <div class="seating-view">
    <div class="page-header">
      <h2>{{ t("seating.title") }}</h2>
      <p class="subtitle">{{ t("seating.subtitle") }}</p>
    </div>

    <div v-if="loading" class="loading">{{ t("common.loading") }}</div>
    <div v-else-if="!layout || layout.tables.length === 0" class="empty">
      {{ t("seating.noTables") }}
    </div>
    <div v-else class="tables-grid">
      <div v-for="table in layout.tables" :key="table.id" class="table-card">
        <h4 class="table-name">{{ table.name }}</h4>
        <p class="table-capacity">
          {{ t("seating.capacity") }}: {{ table.capacity }}
        </p>
        <div class="seat-list">
          <div v-for="seat in table.seats" :key="seat" class="seat">
            <span v-if="seat">{{ seat }}</span>
            <span v-else class="empty-seat">{{ t("seating.emptySeat") }}</span>
          </div>
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
.loading,
.empty {
  color: var(--color-muted);
  text-align: center;
  padding: 48px 0;
}
.tables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 18px;
}
.table-card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 18px;
}
.table-name {
  margin: 0 0 4px;
  font-size: 1rem;
  font-weight: 700;
}
.table-capacity {
  margin: 0 0 12px;
  font-size: 0.8rem;
  color: var(--color-muted);
}
.seat-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.seat {
  padding: 6px 10px;
  background: var(--color-surface);
  border-radius: 6px;
  font-size: 0.85rem;
}
.empty-seat {
  color: var(--color-muted);
  font-style: italic;
}
</style>
