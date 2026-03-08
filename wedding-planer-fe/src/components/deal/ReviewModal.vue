<script setup lang="ts">
import { ref } from "vue";
import { reviewsApi, type Review } from "@/api/reviews.api";
import { X } from "lucide-vue-next";

const props = defineProps<{
  bookingId: string;
  vendorName: string;
}>();

const emit = defineEmits<{
  close: [];
  submitted: [review: Review];
}>();

const rating = ref(0);
const hovered = ref(0);
const comment = ref("");
const submitting = ref(false);
const error = ref("");

async function submit() {
  if (rating.value === 0) {
    error.value = "Please select a star rating.";
    return;
  }
  error.value = "";
  submitting.value = true;
  try {
    const res = await reviewsApi.create(
      props.bookingId,
      rating.value,
      comment.value,
    );
    emit("submitted", res.data);
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    error.value = msg || "Failed to submit review. Please try again.";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal">
      <button class="close-btn" @click="emit('close')"><X :size="18" /></button>
      <h3 class="modal-title">Leave a Review</h3>
      <p class="modal-sub">
        How was your experience with <strong>{{ vendorName }}</strong
        >?
      </p>

      <!-- Star picker -->
      <div class="stars">
        <button
          v-for="n in 5"
          :key="n"
          class="star-btn"
          :class="{ filled: n <= (hovered || rating) }"
          @mouseenter="hovered = n"
          @mouseleave="hovered = 0"
          @click="rating = n"
          type="button"
        >
          ★
        </button>
      </div>
      <p class="rating-label" v-if="rating > 0">
        {{ ["", "Poor", "Fair", "Good", "Very good", "Excellent"][rating] }}
      </p>

      <!-- Comment -->
      <textarea
        v-model="comment"
        class="comment-input"
        placeholder="Share details about your experience (optional)"
        rows="4"
      />

      <p v-if="error" class="error-msg">{{ error }}</p>

      <div class="modal-actions">
        <button
          class="cancel-btn"
          @click="emit('close')"
          :disabled="submitting"
        >
          Cancel
        </button>
        <button
          class="submit-btn"
          @click="submit"
          :disabled="submitting || rating === 0"
        >
          {{ submitting ? "Submitting…" : "Submit Review" }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: var(--color-white);
  border-radius: 16px;
  padding: 32px 28px 24px;
  width: 100%;
  max-width: 440px;
  position: relative;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.18);
}
.close-btn {
  position: absolute;
  top: 14px;
  right: 16px;
  background: none;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  color: var(--color-muted);
  line-height: 1;
}
.modal-title {
  margin: 0 0 6px;
  font-size: 1.25rem;
  font-weight: 700;
}
.modal-sub {
  margin: 0 0 20px;
  color: var(--color-muted);
  font-size: 0.9rem;
}
.stars {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}
.star-btn {
  background: none;
  border: none;
  font-size: 2rem;
  cursor: pointer;
  color: var(--color-border);
  transition:
    color 0.1s,
    transform 0.1s;
  padding: 0;
  line-height: 1;
}
.star-btn.filled {
  color: var(--color-gold);
}
.star-btn:hover {
  transform: scale(1.15);
}
.rating-label {
  margin: 0 0 16px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-gold);
}
.comment-input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 0.9rem;
  font-family: inherit;
  resize: vertical;
  background: var(--color-bg);
  color: var(--color-text);
  box-sizing: border-box;
  margin-bottom: 16px;
}
.comment-input:focus {
  outline: none;
  border-color: var(--color-gold);
}
.error-msg {
  color: var(--color-error);
  font-size: 0.83rem;
  margin: 0 0 12px;
}
.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.cancel-btn {
  background: none;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  padding: 8px 18px;
  font-size: 0.88rem;
  cursor: pointer;
  color: var(--color-text);
}
.submit-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 20px;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s;
}
.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
