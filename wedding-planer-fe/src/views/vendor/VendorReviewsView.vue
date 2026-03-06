<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { reviewsApi } from "@/api/reviews.api";
import RatingStars from "@/components/ui/RatingStars.vue";
import type { Review } from "@/api/reviews.api";

const { t } = useI18n();

const reviews = ref<Review[]>([]);
const loading = ref(false);
const replyText = ref<Record<string, string>>({});
const replying = ref<Record<string, boolean>>({});

onMounted(async () => {
  loading.value = true;
  try {
    reviews.value = (await reviewsApi.list()).data;
  } finally {
    loading.value = false;
  }
});

async function submitReply(reviewId: string) {
  replying.value[reviewId] = true;
  await reviewsApi.reply(reviewId, replyText.value[reviewId] ?? "");
  const r = reviews.value.find((r) => r.id === reviewId);
  if (r) r.vendorReply = replyText.value[reviewId];
  replying.value[reviewId] = false;
}
</script>

<template>
  <div class="reviews-view">
    <h2>{{ t("vendor.reviews.title") }}</h2>
    <div v-if="loading" class="loading">{{ t("common.loading") }}</div>
    <div v-else-if="reviews.length === 0" class="empty">
      {{ t("vendor.reviews.noReviews") }}
    </div>
    <div v-else class="review-list">
      <div v-for="review in reviews" :key="review.id" class="review-card">
        <div class="review-header">
          <div>
            <p class="couple-name">{{ review.coupleName }}</p>
            <p class="review-date">
              {{ new Date(review.createdAt).toLocaleDateString() }}
            </p>
          </div>
          <RatingStars :rating="review.rating" />
        </div>
        <p class="review-text">{{ review.comment }}</p>
        <div v-if="review.vendorReply" class="vendor-reply">
          <strong>{{ t("vendor.reviews.reply") }}:</strong>
          {{ review.vendorReply }}
        </div>
        <div v-else class="reply-form">
          <textarea
            v-model="replyText[review.id]"
            class="reply-input"
            :placeholder="t('vendor.reviews.respondToReview')"
            rows="2"
          />
          <button
            class="reply-btn"
            :disabled="replying[review.id] || !replyText[review.id]?.trim()"
            @click="submitReply(review.id)"
          >
            {{ t("vendor.reviews.respondToReview") }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
h2 {
  margin: 0 0 24px;
  font-size: 1.4rem;
}
.loading,
.empty {
  color: var(--color-muted);
  text-align: center;
  padding: 48px;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.review-card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 20px;
}
.review-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 10px;
}
.couple-name {
  margin: 0 0 2px;
  font-weight: 700;
  font-size: 0.9rem;
}
.review-date {
  margin: 0;
  font-size: 0.78rem;
  color: var(--color-muted);
}
.review-text {
  margin: 0 0 14px;
  font-size: 0.9rem;
  line-height: 1.5;
  color: var(--color-text);
}
.vendor-reply {
  background: var(--color-surface);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 0.85rem;
  color: var(--color-text);
}
.reply-form {
  display: flex;
  gap: 10px;
}
.reply-input {
  flex: 1;
  padding: 8px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.85rem;
  font-family: inherit;
  outline: none;
  resize: vertical;
}
.reply-input:focus {
  border-color: var(--color-gold);
}
.reply-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 700;
  cursor: pointer;
  height: fit-content;
}
.reply-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
