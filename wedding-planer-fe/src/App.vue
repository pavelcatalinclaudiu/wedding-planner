<script setup lang="ts">
import { onMounted, onUnmounted } from "vue";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import VideoCallModal from "@/components/video/VideoCallModal.vue";
import ToastContainer from "@/components/ui/ToastContainer.vue";
import ConfirmModal from "@/components/ui/ConfirmModal.vue";

const videoStore = useVideoCallsStore();

function handleGlobalEscape(e: KeyboardEvent) {
  if (e.key !== "Escape") return;
  // Click backdrop of any open inline modal so its @click.self handler fires
  const overlay = document.querySelector<HTMLElement>(
    ".modal-overlay, .modal-backdrop, .confirm-backdrop",
  );
  if (overlay) {
    overlay.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  }
}

onMounted(() => document.addEventListener("keydown", handleGlobalEscape));
onUnmounted(() => document.removeEventListener("keydown", handleGlobalEscape));
</script>

<template>
  <RouterView />
  <VideoCallModal v-if="videoStore.showCallModal" />
  <ToastContainer />
  <ConfirmModal />
</template>

<style>
@import url("https://fonts.googleapis.com/css2?family=Cinzel:wght@400;500;600&family=Raleway:ital,wght@0,300;0,400;0,500;1,300;1,400&display=swap");

*,
*::before,
*::after {
  box-sizing: border-box;
}
html {
  font-family:
    "Raleway",
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-weight: 400;
}
body {
  margin: 0;
  font-family: inherit;
}
input,
textarea,
button,
select,
a {
  font-family: inherit;
  font-size: inherit;
}
h1,
h2,
h3,
h4,
h5,
h6 {
  font-family: "Cinzel", Georgia, serif;
  font-weight: 500;
  letter-spacing: 0.04em;
}
</style>
