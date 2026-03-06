import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { budgetApi } from "@/api/budget.api";
import type { BudgetItem } from "@/types/couple.types";
import { useToast } from "@/composables/useToast";

export const useBudgetStore = defineStore("budget", () => {
  const items = ref<BudgetItem[]>([]);
  const loading = ref(false);

  const totalBudgeted = computed(() =>
    items.value.reduce((sum, i) => sum + Number(i.estimatedCost ?? 0), 0),
  );
  const totalActual = computed(() =>
    items.value.reduce((sum, i) => sum + Number(i.actualCost ?? 0), 0),
  );
  const totalPaid = computed(() =>
    items.value
      .filter((i) => i.isPaid)
      .reduce((sum, i) => sum + Number(i.estimatedCost ?? 0), 0),
  );

  async function fetchBudget() {
    loading.value = true;
    try {
      const res = await budgetApi.list();
      items.value = res.data;
    } finally {
      loading.value = false;
    }
  }

  async function addItem(data: Partial<BudgetItem>) {
    const res = await budgetApi.add(data);
    items.value.push(res.data);
    useToast().success("Budget item added");
    return res.data;
  }

  async function updateItem(id: string, data: Partial<BudgetItem>) {
    const res = await budgetApi.update(id, data);
    const idx = items.value.findIndex((b) => b.id === id);
    if (idx !== -1) items.value[idx] = res.data;
    useToast().success("Budget item updated");
    return res.data;
  }

  async function deleteItem(id: string) {
    await budgetApi.remove(id);
    items.value = items.value.filter((b) => b.id !== id);
    useToast().success("Budget item deleted");
  }

  return {
    items,
    loading,
    totalBudgeted,
    totalActual,
    totalPaid,
    fetchBudget,
    addItem,
    updateItem,
    deleteItem,
  };
});
