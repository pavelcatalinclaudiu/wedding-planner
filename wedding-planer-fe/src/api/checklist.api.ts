import apiClient from "./client";
import type { ChecklistItem } from "@/types/couple.types";

export const checklistApi = {
  list: () => apiClient.get<ChecklistItem[]>("/checklist"),
  add: (data: {
    label: string;
    dueDate?: string;
    timePeriod?: string;
    notes?: string;
  }) =>
    apiClient.post<ChecklistItem>("/checklist", {
      label: data.label,
      dueDate: data.dueDate,
      timePeriod: data.timePeriod,
      notes: data.notes,
    }),
  update: (id: string, data: Partial<ChecklistItem> & { notes?: string }) =>
    apiClient.put<ChecklistItem>(`/checklist/${id}`, {
      label: data.label,
      dueDate: data.dueDate,
      timePeriod: data.timePeriod,
      notes: data.notes,
    }),
  toggle: (id: string) =>
    apiClient.post<ChecklistItem>(`/checklist/${id}/toggle`, {}),
  remove: (id: string) => apiClient.delete(`/checklist/${id}`),
};
