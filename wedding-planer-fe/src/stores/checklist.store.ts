import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { checklistApi } from "@/api/checklist.api";
import type { ChecklistItem } from "@/types/couple.types";
import { useToast } from "@/composables/useToast";
import { isWithinInterval, addDays } from "date-fns";

export const useChecklistStore = defineStore("checklist", () => {
  const items = ref<ChecklistItem[]>([]);
  const loading = ref(false);

  const done = computed(() => items.value.filter((i) => i.done).length);
  const total = computed(() => items.value.length);

  const urgent = computed(
    () =>
      items.value.filter((i) => {
        if (i.done || !i.dueDate) return false;
        const due = new Date(i.dueDate);
        return isWithinInterval(due, {
          start: new Date(),
          end: addDays(new Date(), 7),
        });
      }).length,
  );

  const overdue = computed(
    () =>
      items.value.filter((i) => {
        if (i.done || !i.dueDate) return false;
        return new Date(i.dueDate) < new Date();
      }).length,
  );

  const byTimePeriod = computed(() => {
    const map: Record<string, ChecklistItem[]> = {};
    items.value.forEach((i) => {
      const period = i.timePeriod || "General";
      if (!map[period]) map[period] = [];
      map[period].push(i);
    });
    return map;
  });

  async function fetchChecklist() {
    loading.value = true;
    try {
      const res = await checklistApi.list();
      items.value = res.data;
    } finally {
      loading.value = false;
    }
  }

  async function toggleItem(id: string) {
    const res = await checklistApi.toggle(id);
    const idx = items.value.findIndex((i) => i.id === id);
    if (idx !== -1) items.value[idx] = res.data;
  }

  async function addItem(data: {
    label: string;
    dueDate?: string;
    timePeriod?: string;
    notes?: string;
  }) {
    const res = await checklistApi.add(data);
    items.value.push(res.data);
    useToast().success("Task added");
  }

  async function updateItem(
    id: string,
    data: Partial<ChecklistItem> & { notes?: string },
  ) {
    const res = await checklistApi.update(id, data);
    const idx = items.value.findIndex((i) => i.id === id);
    if (idx !== -1) items.value[idx] = res.data;
    useToast().success("Task updated");
  }

  async function removeItem(id: string) {
    await checklistApi.remove(id);
    items.value = items.value.filter((i) => i.id !== id);
    useToast().success("Task removed");
  }

  return {
    items,
    loading,
    done,
    total,
    urgent,
    overdue,
    byTimePeriod,
    fetchChecklist,
    toggleItem,
    addItem,
    updateItem,
    removeItem,
  };
});
