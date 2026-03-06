import { reactive } from "vue";

export type ToastType = "success" | "error" | "warn" | "info";

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
}

// Module-level singleton — usable from stores AND components without Pinia
const toasts = reactive<Toast[]>([]);
let _nextId = 0;

function show(message: string, type: ToastType = "info", duration = 3_500) {
  const id = ++_nextId;
  toasts.push({ id, message, type });
  setTimeout(() => dismiss(id), duration);
}

function dismiss(id: number) {
  const i = toasts.findIndex((t) => t.id === id);
  if (i !== -1) toasts.splice(i, 1);
}

export function useToast() {
  return {
    toasts,
    success: (msg: string) => show(msg, "success"),
    error: (msg: string) => show(msg, "error", 5_000),
    warn: (msg: string) => show(msg, "warn"),
    info: (msg: string) => show(msg, "info"),
    dismiss,
  };
}
