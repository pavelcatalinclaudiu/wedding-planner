import axios from "axios";
import router from "@/router";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// Attach JWT to every request
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// On 401 for non-auth endpoints, clear auth state and redirect to login
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const url: string = error.config?.url ?? "";
    // Skip auth endpoints to avoid infinite recursion (logout → 401 → logout → …)
    if (error.response?.status === 401 && !url.includes("/auth/")) {
      const { useAuthStore } = await import("@/stores/auth.store");
      useAuthStore().logout();
      router.push("/login");
    }
    return Promise.reject(error);
  },
);

export default apiClient;
