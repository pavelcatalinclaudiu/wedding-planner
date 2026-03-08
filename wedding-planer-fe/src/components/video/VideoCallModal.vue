<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import { useVideoCall } from "@/composables/useVideoCall";
import { AlertTriangle } from "lucide-vue-next";

const videoStore = useVideoCallsStore();
const { join, leave, jitsiError } = useVideoCall();
const joinError = ref("");
const connecting = ref(true);

onMounted(async () => {
  const roomUrl = videoStore.activeCall?.roomUrl;
  if (!roomUrl) {
    // No room URL — can't connect. Show actionable error immediately.
    joinError.value =
      "No room URL available. Please try starting the call again.";
    connecting.value = false;
    return;
  }
  try {
    await join(roomUrl, videoStore.activeCall?.token);
    connecting.value = false;
  } catch (e: any) {
    joinError.value =
      e?.message ?? jitsiError.value ?? "Failed to connect to video call";
    connecting.value = false;
  }
});

async function endCall() {
  leave();
  videoStore.closeCallModal();
}
</script>

<template>
  <div class="video-modal-overlay">
    <div class="video-modal">
      <div class="video-header">
        <span class="call-title">
          Video Call —
          {{
            videoStore.activeCall?.coupleName ??
            videoStore.activeCall?.vendorName ??
            "In progress"
          }}
        </span>
        <button class="end-call-btn" @click="endCall">End Call</button>
      </div>
      <div class="call-frame">
        <div v-if="joinError" class="call-error">
          <p><AlertTriangle :size="16" /> {{ joinError }}</p>
          <button @click="endCall" class="end-call-btn">Close</button>
        </div>
        <div v-else-if="connecting" class="call-connecting">
          <p>Connecting to video call…</p>
        </div>
        <div
          id="jitsi-call-container"
          style="position: absolute; inset: 0"
        ></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 500;
}
.video-modal {
  background: #1a1a1a;
  border-radius: 16px;
  width: 90vw;
  max-width: 960px;
  height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.video-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: #111;
}
.call-title {
  color: #fff;
  font-size: 0.95rem;
  font-weight: 600;
}
.end-call-btn {
  background: #e74c3c;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 18px;
  font-weight: 700;
  cursor: pointer;
}
.call-frame {
  flex: 1;
  position: relative;
  /* Explicit height so percentage-height children (Jitsi iframe) resolve correctly */
  min-height: 0;
  height: 0; /* flex:1 overrides this, but it anchors height:100% children */
  overflow: hidden;
}
.call-connecting,
.call-error {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 0.95rem;
  gap: 12px;
  background: #111;
  z-index: 1;
}
.call-error {
  color: #e74c3c;
}
</style>
