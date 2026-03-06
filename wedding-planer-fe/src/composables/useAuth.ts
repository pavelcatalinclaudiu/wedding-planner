import { computed } from "vue";
import { useAuthStore } from "@/stores/auth.store";
import { useRouter } from "vue-router";

export function useAuth() {
  const store = useAuthStore();
  const router = useRouter();

  const user = computed(() => store.user);
  const isAuthenticated = computed(() => store.isAuthenticated);
  const isCouple = computed(() => store.user?.role === "COUPLE");
  const isVendor = computed(() => store.user?.role === "VENDOR");

  async function login(email: string, password: string) {
    const data = await store.login({ email, password });
    if (data.user.role === "COUPLE") {
      router.push("/couple/overview");
    } else {
      router.push("/vendor/overview");
    }
  }

  async function logout() {
    await store.logout();
    router.push("/login");
  }

  function redirectByRole() {
    if (store.user?.role === "COUPLE") router.push("/couple");
    else router.push("/vendor");
  }

  return {
    user,
    isAuthenticated,
    isCouple,
    isVendor,
    login,
    logout,
    redirectByRole,
  };
}
