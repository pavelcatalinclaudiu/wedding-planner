import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { authApi } from "@/api/auth.api";
import { useLeadsStore } from "@/stores/leads.store";
import { useMessagesStore } from "@/stores/messages.store";
import { useNotificationsStore } from "@/stores/notifications.store";
import { useVendorOverviewStore } from "@/stores/vendorOverview.store";
import { useCoupleStore } from "@/stores/couple.store";
import { useGuestsStore } from "@/stores/guests.store";
import type { User, LoginRequest, RegisterRequest } from "@/types/user.types";

export const useAuthStore = defineStore("auth", () => {
  const _storedUser = localStorage.getItem("auth_user");
  const user = ref<User | null>(
    _storedUser ? (JSON.parse(_storedUser) as User) : null,
  );
  const accessToken = ref<string | null>(localStorage.getItem("access_token"));

  const isAuthenticated = computed(() => !!accessToken.value && !!user.value);

  async function login(data: LoginRequest) {
    const res = await authApi.login(data);
    user.value = res.data.user;
    accessToken.value = res.data.accessToken;
    localStorage.setItem("access_token", res.data.accessToken);
    localStorage.setItem("auth_user", JSON.stringify(res.data.user));
    // Reset overview so a newly-logged-in vendor never sees a previous vendor's stats.
    useVendorOverviewStore().reset();
    return res.data;
  }

  async function register(data: RegisterRequest) {
    const res = await authApi.register(data);
    user.value = res.data.user;
    accessToken.value = res.data.accessToken;
    localStorage.setItem("access_token", res.data.accessToken);
    localStorage.setItem("auth_user", JSON.stringify(res.data.user));
    return res.data;
  }

  function logout() {
    const leadsStore = useLeadsStore();
    const messagesStore = useMessagesStore();
    const notificationsStore = useNotificationsStore();
    const vendorOverviewStore = useVendorOverviewStore();

    leadsStore.reset();
    messagesStore.reset();
    notificationsStore.reset();
    vendorOverviewStore.reset();
    useCoupleStore().reset();
    useGuestsStore().reset();

    // Clear state synchronously first — no await, no network call that could
    // re-trigger the 401 interceptor and cause infinite recursion.
    user.value = null;
    accessToken.value = null;
    localStorage.removeItem("access_token");
    localStorage.removeItem("auth_user");
    // Fire-and-forget: notify the backend but don't block or retry on failure.
    authApi.logout().catch(() => {});
  }

  async function fetchCurrentUser() {
    try {
      const res = await authApi.me();
      user.value = res.data;
      localStorage.setItem("auth_user", JSON.stringify(res.data));
    } catch {
      // Silently ignore — token may be expired but we don't force logout here.
      // The axios 401 interceptor handles logout when actual API calls fail.
    }
  }

  async function verifyEmail(token: string) {
    return authApi.verifyEmail(token);
  }

  async function requestPasswordReset(email: string) {
    return authApi.requestPasswordReset(email);
  }

  async function resetPassword(token: string, password: string) {
    return authApi.resetPassword(token, password);
  }

  // Synchronous — user is already restored from localStorage above.
  // Returns a promise only for callers that need to await a background token refresh.
  function init(): Promise<void> {
    return accessToken.value ? fetchCurrentUser() : Promise.resolve();
  }

  return {
    user,
    accessToken,
    isAuthenticated,
    login,
    register,
    logout,
    fetchCurrentUser,
    verifyEmail,
    requestPasswordReset,
    resetPassword,
    init,
  };
});
