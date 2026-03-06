import { defineStore } from "pinia";
import { ref } from "vue";
import { coupleApi } from "@/api/couple.api";
import type {
  CoupleProfile,
  ChecklistItem,
  BudgetItem,
} from "@/types/couple.types";

export const useCoupleStore = defineStore("couple", () => {
  const profile = ref<CoupleProfile | null>(null);
  const checklist = ref<ChecklistItem[]>([]);
  const budget = ref<BudgetItem[]>([]);
  const loading = ref(false);

  async function fetchProfile() {
    loading.value = true;
    try {
      const res = await coupleApi.getProfile();
      profile.value = res.data;
    } finally {
      loading.value = false;
    }
  }

  async function updateProfile(data: Partial<CoupleProfile>) {
    const res = await coupleApi.updateProfile(data);
    profile.value = res.data;
  }

  async function fetchChecklist() {
    const res = await coupleApi.getChecklist();
    checklist.value = res.data;
  }

  async function toggleChecklistItem(id: string, done: boolean) {
    const res = await coupleApi.updateChecklistItem(id, done);
    const idx = checklist.value.findIndex((i) => i.id === id);
    if (idx !== -1) checklist.value[idx] = res.data;
  }

  async function addChecklistItem(
    label: string,
    dueDate?: string,
    timePeriod?: string,
  ) {
    const res = await coupleApi.addChecklistItem(label, dueDate, timePeriod);
    checklist.value.push(res.data);
  }

  async function fetchBudget() {
    const res = await coupleApi.getBudget();
    budget.value = res.data;
  }

  async function addBudgetItem(data: Partial<BudgetItem>) {
    const res = await coupleApi.addBudgetItem(data);
    budget.value.push(res.data);
  }

  async function updateBudgetItem(id: string, data: Partial<BudgetItem>) {
    const res = await coupleApi.updateBudgetItem(id, data);
    const idx = budget.value.findIndex((b) => b.id === id);
    if (idx !== -1) budget.value[idx] = res.data;
  }

  async function deleteBudgetItem(id: string) {
    await coupleApi.deleteBudgetItem(id);
    budget.value = budget.value.filter((b) => b.id !== id);
  }

  return {
    profile,
    checklist,
    budget,
    loading,
    fetchProfile,
    updateProfile,
    fetchChecklist,
    toggleChecklistItem,
    addChecklistItem,
    fetchBudget,
    addBudgetItem,
    updateBudgetItem,
    deleteBudgetItem,
  };
});
