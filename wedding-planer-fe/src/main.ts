import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "./assets/main.css";
import { useToast } from "@/composables/useToast";

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.mount("#app");

// Global handler for unhandled promise rejections — shows a toast instead of silently failing
window.addEventListener("unhandledrejection", (event) => {
  const msg =
    (event.reason as { message?: string })?.message ??
    "An unexpected error occurred.";
  // Suppress 401 errors — the axios interceptor already handles those
  if (!String(msg).includes("401")) {
    useToast().error(msg);
  }
});
