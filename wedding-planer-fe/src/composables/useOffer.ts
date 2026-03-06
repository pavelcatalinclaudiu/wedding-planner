import { ref, onUnmounted } from "vue";
import { offersApi } from "@/api/offers.api";
import { useLeadsStore } from "@/stores/leads.store";
import { useWebSocket } from "@/composables/useWebSocket";
import type { Offer, CreateOfferPayload } from "@/types/offer.types";

export function useOffer(callbacks?: { onLeadUpdated?: () => void }) {
  const offers = ref<Offer[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  let currentLeadId: string | null = null;
  const ws = useWebSocket();

  // Reload only data — does NOT reconnect WS (safe to call from WS handler)
  async function reloadData() {
    if (!currentLeadId) return;
    try {
      offers.value = (await offersApi.listByLead(currentLeadId)).data;
    } catch {
      /* silent */
    }
  }

  // When the backend broadcasts OFFER_UPDATED, patch/append directly from the
  // embedded DTO — no re-fetch, no race against DB commit.
  const offOfferUpdated = ws.on("OFFER_UPDATED", (payload) => {
    const incoming = payload as unknown as Offer;
    if (!incoming?.id) {
      // Legacy fallback: plain string payload — do a full reload
      reloadData();
    } else {
      const idx = offers.value.findIndex((o) => o.id === incoming.id);
      if (idx !== -1) {
        offers.value[idx] = incoming; // update existing
      } else {
        offers.value.unshift(incoming); // new offer from vendor
      }
    }
    callbacks?.onLeadUpdated?.();
  });

  // useOffer does NOT own the WS connection — useConversation connects/disconnects.
  // We only register a handler and clean it up on unmount.
  onUnmounted(() => {
    offOfferUpdated();
  });

  async function loadForLead(leadId: string) {
    currentLeadId = leadId;
    loading.value = true;
    error.value = null;
    try {
      offers.value = (await offersApi.listByLead(leadId)).data;
    } catch (e: any) {
      error.value = e?.response?.data?.message ?? "Failed to load offers";
    } finally {
      loading.value = false;
    }
  }

  async function createOffer(payload: CreateOfferPayload) {
    const offer = (await offersApi.create(payload)).data;
    // Guard against duplicate: WS may have already inserted it before HTTP response returned
    if (!offers.value.some((o) => o.id === offer.id)) {
      offers.value.unshift(offer);
    }
    return offer;
  }

  async function accept(offerId: string) {
    const updated = (await offersApi.accept(offerId)).data;
    _patch(updated);
    // Refresh leads store so the BOOKED status is visible in the wedding team view
    try {
      await useLeadsStore().fetchCoupleLeads();
    } catch (_) {}
    callbacks?.onLeadUpdated?.();
    return updated;
  }

  async function decline(offerId: string) {
    const updated = (await offersApi.decline(offerId)).data;
    _patch(updated);
    callbacks?.onLeadUpdated?.();
    return updated;
  }

  async function requestRevision(offerId: string) {
    const updated = (await offersApi.requestRevision(offerId)).data;
    _patch(updated);
    callbacks?.onLeadUpdated?.();
    return updated;
  }

  function _patch(updated: Offer) {
    const idx = offers.value.findIndex((o) => o.id === updated.id);
    if (idx !== -1) offers.value[idx] = updated;
  }

  return {
    offers,
    loading,
    error,
    loadForLead,
    createOffer,
    accept,
    decline,
    requestRevision,
  };
}
