import apiClient from "./client";
import type { BudgetItem } from "@/types/couple.types";

export const budgetApi = {
  list: () => apiClient.get<BudgetItem[]>("/budget"),
  add: (data: Partial<BudgetItem>) =>
    apiClient.post<BudgetItem>("/budget", data),
  update: (id: string, data: Partial<BudgetItem>) =>
    apiClient.put<BudgetItem>(`/budget/${id}`, data),
  remove: (id: string) => apiClient.delete(`/budget/${id}`),
};
