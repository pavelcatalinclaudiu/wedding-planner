import { useRouter } from "vue-router";

const INTENT_KEY = "eternelle_enquiry_intent";
const REDIRECT_KEY = "eternelle_post_auth_redirect";

export interface EnquiryIntent {
  vendorId: string;
  vendorName: string;
  vendorCategory: string;
  vendorCity: string;
  vendorPhotoUrl: string;
  packageInterest: string | null;
  returnPath: string;
  savedAt: number;
}

export function useAuthRedirect() {
  const router = useRouter();

  // ── Enquiry intent ────────────────────────────────────────────────────────

  function saveEnquiryIntent(intent: Omit<EnquiryIntent, "savedAt">) {
    sessionStorage.setItem(
      INTENT_KEY,
      JSON.stringify({ ...intent, savedAt: Date.now() }),
    );
  }

  function getEnquiryIntent(): EnquiryIntent | null {
    const raw = sessionStorage.getItem(INTENT_KEY);
    if (!raw) return null;
    const intent = JSON.parse(raw) as EnquiryIntent;
    // Expire intents older than 30 minutes
    if (Date.now() - intent.savedAt > 30 * 60 * 1000) {
      clearEnquiryIntent();
      return null;
    }
    return intent;
  }

  function clearEnquiryIntent() {
    sessionStorage.removeItem(INTENT_KEY);
  }

  function hasEnquiryIntent(): boolean {
    return getEnquiryIntent() !== null;
  }

  // ── Generic return path ────────────────────────────────────────────────────
  // Used when a protected page forces a /login redirect

  function saveReturnPath(path: string) {
    sessionStorage.setItem(REDIRECT_KEY, path);
  }

  function getReturnPath(): string | null {
    return sessionStorage.getItem(REDIRECT_KEY);
  }

  function clearReturnPath() {
    sessionStorage.removeItem(REDIRECT_KEY);
  }

  // ── Main redirect decision ─────────────────────────────────────────────────
  // Called after every successful login or registration.

  function redirectAfterAuth(userRole: "COUPLE" | "VENDOR") {
    // Priority 1: enquiry intent — couple was mid-enquiry before auth
    const intent = getEnquiryIntent();
    if (intent && userRole === "COUPLE") {
      clearEnquiryIntent();
      router.push({ path: intent.returnPath, query: { openEnquiry: "true" } });
      return;
    }

    // Priority 2: saved return path — a protected route forced login
    const returnPath = getReturnPath();
    if (returnPath) {
      clearReturnPath();
      router.push(returnPath);
      return;
    }

    // Priority 3: default — their dashboard
    router.push(
      userRole === "COUPLE" ? "/couple/overview" : "/vendor/overview",
    );
  }

  return {
    saveEnquiryIntent,
    getEnquiryIntent,
    clearEnquiryIntent,
    hasEnquiryIntent,
    saveReturnPath,
    getReturnPath,
    clearReturnPath,
    redirectAfterAuth,
  };
}
