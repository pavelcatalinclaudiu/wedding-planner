import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { guestsApi } from "@/api/guests.api";
import type { Guest, GuestRequest, GuestStats } from "@/types/couple.types";
import { useToast } from "@/composables/useToast";

export const useGuestsStore = defineStore("guests", () => {
  const guests = ref<Guest[]>([]);
  const stats = ref<GuestStats | null>(null);
  const songs = ref<string[]>([]);
  const loading = ref(false);

  // ── computed ─────────────────────────────────────────────────
  const total = computed(() => guests.value.length);
  const confirmed = computed(
    () => guests.value.filter((g) => g.rsvpStatus === "CONFIRMED").length,
  );
  const declined = computed(
    () => guests.value.filter((g) => g.rsvpStatus === "DECLINED").length,
  );
  const pending = computed(
    () => guests.value.filter((g) => g.rsvpStatus === "PENDING").length,
  );
  const maybe = computed(
    () => guests.value.filter((g) => g.rsvpStatus === "MAYBE").length,
  );

  // ── actions ────────────────────────────────────────────────
  async function fetchGuests() {
    loading.value = true;
    try {
      const [gRes, sRes] = await Promise.all([
        guestsApi.list(),
        guestsApi.getStats(),
      ]);
      guests.value = gRes.data;
      stats.value = sRes.data;
    } finally {
      loading.value = false;
    }
  }

  async function fetchSongs() {
    const res = await guestsApi.getSongs();
    songs.value = res.data;
  }

  async function addGuest(data: GuestRequest) {
    const res = await guestsApi.create(data);
    guests.value.push(res.data);
    await refreshStats();
    useToast().success("Guest added");
    return res.data;
  }

  async function updateGuest(id: string, data: GuestRequest) {
    const res = await guestsApi.update(id, data);
    const idx = guests.value.findIndex((g) => g.id === id);
    if (idx !== -1) guests.value[idx] = res.data;
    await refreshStats();
    return res.data;
  }

  async function removeGuest(id: string) {
    await guestsApi.remove(id);
    guests.value = guests.value.filter((g) => g.id !== id);
    await refreshStats();
    useToast().success("Guest removed");
  }

  async function importCsv(csv: string) {
    const res = await guestsApi.importCsv(csv);
    await fetchGuests(); // reload everything after import
    useToast().success(`${res.data.imported} guests imported`);
    return res.data.imported;
  }

  async function refreshStats() {
    const res = await guestsApi.getStats();
    stats.value = res.data;
  }

  return {
    guests,
    stats,
    songs,
    loading,
    total,
    confirmed,
    declined,
    pending,
    maybe,
    fetchGuests,
    fetchSongs,
    addGuest,
    updateGuest,
    removeGuest,
    importCsv,
  };
});
