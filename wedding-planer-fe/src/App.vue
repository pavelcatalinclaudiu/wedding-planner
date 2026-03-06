<script setup lang="ts">
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import VideoCallModal from "@/components/video/VideoCallModal.vue";
import ToastContainer from "@/components/ui/ToastContainer.vue";
import ConfirmModal from "@/components/ui/ConfirmModal.vue";

const videoStore = useVideoCallsStore();
</script>

<template>
  <RouterView />
  <VideoCallModal v-if="videoStore.showCallModal" />
  <ToastContainer />
  <ConfirmModal />
</template>

<style>
*,
*::before,
*::after {
  box-sizing: border-box;
}
body {
  margin: 0;
  font-family:
    "Inter",
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
}
</style>
