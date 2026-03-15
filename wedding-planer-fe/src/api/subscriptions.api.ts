import apiClient from "./client";

export interface Subscription {
  id: string;
  vendorId: string;
  stripeSubscriptionId?: string;
  plan: "FREE" | "STANDARD" | "PREMIUM" | "DREAM_WEDDING";
  status: "active" | "cancelled" | "past_due";
  cancelAtPeriodEnd?: boolean;
  renewalDate?: string;
  currentPeriodEnd?: string;
  videoCallsUsed: number;
  videoCallsLimit: number;
  creditsBalance: number;
}

export const subscriptionsApi = {
  get: () => apiClient.get<Subscription>("/subscriptions/me"),
  checkout: (
    plan: string,
    billing: "monthly" | "onetime" = "monthly",
    successUrl?: string,
    cancelUrl?: string,
  ) =>
    apiClient.post<{ url: string }>("/subscriptions/checkout", {
      plan,
      billing,
      successUrl,
      cancelUrl,
    }),
  portal: (returnUrl?: string) =>
    apiClient.post<{ url: string }>("/subscriptions/portal", { returnUrl }),
  cancel: () => apiClient.post("/subscriptions/cancel"),
  syncSession: (sessionId: string) =>
    apiClient.post<Subscription | null>("/subscriptions/sync-session", {
      sessionId,
    }),
  sync: () => apiClient.post<Subscription | null>("/subscriptions/sync"),
  upgrade: (plan: string) =>
    apiClient.post<Subscription>("/subscriptions/upgrade", { plan }),
};
