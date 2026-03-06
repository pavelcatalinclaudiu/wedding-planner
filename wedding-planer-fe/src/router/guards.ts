import type { Router } from "vue-router";
import { useAuthStore } from "@/stores/auth.store";

const INTENT_KEY = "eternelle_enquiry_intent";
const REDIRECT_KEY = "eternelle_post_auth_redirect";

function hasEnquiryIntent(): boolean {
  try {
    const raw = sessionStorage.getItem(INTENT_KEY);
    if (!raw) return false;
    const intent = JSON.parse(raw);
    return Date.now() - intent.savedAt <= 30 * 60 * 1000;
  } catch {
    return false;
  }
}

function saveReturnPath(path: string) {
  sessionStorage.setItem(REDIRECT_KEY, path);
}

export function setupGuards(router: Router) {
  router.beforeEach((to, _from, next) => {
    const authStore = useAuthStore();

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      // Save where they were trying to go so we can redirect after login
      saveReturnPath(to.fullPath);
      return next("/login");
    }

    if (to.meta.guest && authStore.isAuthenticated) {
      // Allow logged-in users to reach auth pages if they have a pending
      // enquiry intent — so the context banner shows correctly.
      if (hasEnquiryIntent()) return next();
      return next(authStore.user?.role === "COUPLE" ? "/couple" : "/vendor");
    }

    if (to.meta.role && authStore.user?.role !== to.meta.role) {
      return next(authStore.user?.role === "COUPLE" ? "/couple" : "/vendor");
    }

    next();
  });
}

// Extend RouteMeta to include our custom fields
declare module "vue-router" {
  interface RouteMeta {
    requiresAuth?: boolean;
    guest?: boolean;
    role?: "COUPLE" | "VENDOR";
  }
}
