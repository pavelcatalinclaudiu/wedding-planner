import { defineStore } from "pinia";
import { ref } from "vue";
import { videoCallsApi } from "@/api/videoCalls.api";
import type { VideoCall } from "@/types/lead.types";

export const useVideoCallsStore = defineStore("videoCalls", () => {
  const calls = ref<VideoCall[]>([]);
  const activeCall = ref<VideoCall | null>(null);
  const showCallModal = ref(false);
  const showScheduleModal = ref(false);
  const scheduleTargetLeadId = ref<string | null>(null);
  const scheduleTargetVendorId = ref<string | null>(null);
  /** When set, the modal reschedules this call instead of creating a new one. */
  const rescheduleCallId = ref<string | null>(null);
  const liveCallByLead = ref<Record<string, VideoCall | null>>({});

  async function fetchCalls() {
    const res = await videoCallsApi.list();
    calls.value = res.data;
  }

  /** Fetch the active (SCHEDULED / IN_PROGRESS) call for a specific lead. */
  async function fetchActiveForLead(leadId: string) {
    try {
      const res = await videoCallsApi.getActiveForLead(leadId);
      liveCallByLead.value[leadId] = res.data ?? null;
    } catch {
      liveCallByLead.value[leadId] = null;
    }
  }

  async function scheduleCall(
    leadId: string,
    scheduledAt: string,
    postCallAction?: string,
  ) {
    const res = await videoCallsApi.schedule(
      leadId,
      scheduledAt,
      postCallAction,
    );
    calls.value.push(res.data);
    // Update live card for this lead
    liveCallByLead.value[leadId] = res.data;
    return res.data;
  }

  async function acceptCall(callId: string, leadId?: string) {
    const res = await videoCallsApi.accept(callId);
    const idx = calls.value.findIndex((c) => c.id === callId);
    if (idx !== -1) calls.value[idx] = res.data;
    if (leadId) liveCallByLead.value[leadId] = res.data;
    return res.data;
  }

  async function cancelCall(callId: string, leadId?: string) {
    const res = await videoCallsApi.cancel(callId);
    const idx = calls.value.findIndex((c) => c.id === callId);
    if (idx !== -1) calls.value[idx] = res.data;
    if (leadId) liveCallByLead.value[leadId] = null;
    return res.data;
  }

  async function rescheduleCall(
    callId: string,
    scheduledAt: string,
    leadId?: string,
  ) {
    const res = await videoCallsApi.reschedule(callId, scheduledAt);
    const idx = calls.value.findIndex((c) => c.id === callId);
    if (idx !== -1) calls.value[idx] = res.data;
    if (leadId) liveCallByLead.value[leadId] = res.data;
    return res.data;
  }

  async function joinCall(callId: string) {
    const res = await videoCallsApi.start(callId);
    activeCall.value = res.data;
    showCallModal.value = true;
    return res.data;
  }

  async function endCall(callId: string) {
    const res = await videoCallsApi.end(callId);
    const idx = calls.value.findIndex((c) => c.id === callId);
    if (idx !== -1) calls.value[idx] = res.data;
    showCallModal.value = false;
    activeCall.value = null;
    return res.data;
  }

  async function setPostCallAction(callId: string, action: string) {
    return videoCallsApi.setPostCallAction(callId, action);
  }

  function openCallModal(call: VideoCall) {
    activeCall.value = call;
    showCallModal.value = true;
  }

  function closeCallModal() {
    showCallModal.value = false;
    activeCall.value = null;
  }

  function openScheduleModal(
    leadId: string,
    vendorId?: string,
    rescheduleId?: string,
  ) {
    scheduleTargetLeadId.value = leadId;
    scheduleTargetVendorId.value = vendorId ?? null;
    rescheduleCallId.value = rescheduleId ?? null;
    showScheduleModal.value = true;
  }

  function closeScheduleModal() {
    showScheduleModal.value = false;
    scheduleTargetLeadId.value = null;
    scheduleTargetVendorId.value = null;
    rescheduleCallId.value = null;
  }

  return {
    calls,
    activeCall,
    showCallModal,
    showScheduleModal,
    scheduleTargetLeadId,
    scheduleTargetVendorId,
    rescheduleCallId,
    liveCallByLead,
    fetchCalls,
    fetchActiveForLead,
    scheduleCall,
    acceptCall,
    cancelCall,
    rescheduleCall,
    joinCall,
    endCall,
    setPostCallAction,
    openCallModal,
    closeCallModal,
    openScheduleModal,
    closeScheduleModal,
  };
});
