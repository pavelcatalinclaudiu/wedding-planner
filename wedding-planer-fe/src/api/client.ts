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
      router.push("/");
    }
    return Promise.reject(error);
  },
);

/** Extract a human-readable error message from an Axios error.
 *  Handles Quarkus constraint-violation responses ({ violations: [{ message }] })
 *  as well as plain { message } bodies and generic network errors. */
export function extractApiError(e: unknown, fallback: string): string {
  const data = (e as { response?: { data?: Record<string, unknown> } })
    ?.response?.data;
  if (!data) return fallback;
  // Quarkus Bean Validation: { violations: [{ field, message }] }
  if (Array.isArray(data.violations) && data.violations.length > 0) {
    return (data.violations as { message: string }[])
      .map((v) => v.message)
      .join(" · ");
  }
  // Plain error body: { message } or { error } or { title }
  if (typeof data.message === "string" && data.message) return data.message;
  if (typeof data.error === "string" && data.error) return data.error;
  if (typeof data.title === "string" && data.title) return data.title;
  return fallback;
}

export default apiClient;
