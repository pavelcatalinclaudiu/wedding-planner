import apiClient from "./client";

export interface Subscription {
  id: string;
  vendorId: string;
  plan: "FREE" | "GROWTH" | "STUDIO";
  status: "active" | "cancelled" | "past_due";
  renewalDate?: string;
  currentPeriodEnd?: string;
  videoCallsUsed: number;
  videoCallsLimit: number;
  creditsBalance: number;
}

export const subscriptionsApi = {
  get: () => apiClient.get<Subscription>("/subscriptions/me"),
  checkout: (plan: string) =>
    apiClient.post<{ url: string }>("/subscriptions/checkout", { plan }),
  portal: () => apiClient.post<{ url: string }>("/subscriptions/portal"),
  cancel: () => apiClient.post("/subscriptions/cancel"),
};
