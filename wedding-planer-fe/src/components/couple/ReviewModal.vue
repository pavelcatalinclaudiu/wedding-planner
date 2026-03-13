<script setup lang="ts">
import { ref, computed } from "vue";
import { useI18n } from "vue-i18n";
import { reviewsApi } from "@/api/reviews.api";
import { Star, X } from "lucide-vue-next";

const props = defineProps<{
  bookingId: string;
  vendorName: string;
}>();

const emit = defineEmits<{
  (e: "submitted"): void;
  (e: "close"): void;
}>();

const { t } = useI18n();

const hoveredStar = ref(0);
const selectedRating = ref(0);
const comment = ref("");
const submitting = ref(false);
const errorMsg = ref("");

const displayRating = computed(() => hoveredStar.value || selectedRating.value);

const ratingLabels = computed(() => [
  t("review.stars.1"),
  t("review.stars.2"),
  t("review.stars.3"),
  t("review.stars.4"),
  t("review.stars.5"),
]);

const ratingHint = computed(() =>
  displayRating.value ? ratingLabels.value[displayRating.value - 1] : "",
);

const canSubmit = computed(
  () =>
    selectedRating.value > 0 &&
    comment.value.trim().length >= 10 &&
    !submitting.value,
);

async function submit() {
  if (!canSubmit.value) return;
  submitting.value = true;
  errorMsg.value = "";
  try {
    await reviewsApi.create(
      props.bookingId,
      selectedRating.value,
      comment.value.trim(),
    );
    emit("submitted");
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message ?? t("review.submitError");
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="emit('close')">
      <div class="modal">
        <div class="modal-header">
          <div>
            <h2>{{ t("review.title") }}</h2>
            <p class="modal-subtitle">{{ vendorName }}</p>
          </div>
          <button class="close-btn" @click="emit('close')">
            <X :size="18" />
          </button>
        </div>

        <div class="modal-body">
          <!-- Star Rating -->
          <div class="rating-section">
            <p class="section-label">{{ t("review.ratingLabel") }}</p>
            <div class="stars">
              <button
                v-for="n in 5"
                :key="n"
                class="star-btn"
                :class="{ active: n <= displayRating }"
                @mouseenter="hoveredStar = n"
                @mouseleave="hoveredStar = 0"
                @click="selectedRating = n"
              >
                <Star
                  :size="36"
                  :fill="n <= displayRating ? 'currentColor' : 'none'"
                />
              </button>
            </div>
            <p class="rating-hint" :class="{ visible: !!ratingHint }">
              {{ ratingHint || "&nbsp;" }}
            </p>
          </div>

          <!-- Written review -->
          <div class="form-field">
            <label>{{ t("review.commentLabel") }}</label>
            <textarea
              v-model="comment"
              rows="4"
              :placeholder="t('review.commentPlaceholder')"
            ></textarea>
            <p
              class="char-hint"
              :class="{
                warn: comment.trim().length > 0 && comment.trim().length < 10,
                ok: comment.trim().length >= 10,
              }"
            >
              {{ comment.trim().length }} / 10 {{ t("review.charsMin") }}
            </p>
          </div>

          <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
        </div>

        <div class="modal-footer">
          <button class="btn-secondary" @click="emit('close')">
            {{ t("common.cancel") }}
          </button>
          <button class="btn-primary" :disabled="!canSubmit" @click="submit">
            <span v-if="submitting">{{ t("common.saving") }}…</span>
            <span v-else>{{ t("review.submit") }}</span>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 1rem;
}

.modal {
  background: var(--color-white);
  border-radius: 16px;
  width: 100%;
  max-width: 460px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 18px 22px;
  border-bottom: 1px solid var(--color-border);
}

.modal-header h2 {
  font-size: 1.1rem;
  font-weight: 700;
  margin: 0 0 2px;
}

.modal-subtitle {
  font-size: 0.85rem;
  color: var(--color-muted);
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-muted);
  padding: 4px 6px;
  border-radius: 6px;
  transition: background 0.15s;
  flex-shrink: 0;
}

.close-btn:hover {
  background: var(--color-surface-alt);
  color: var(--color-text);
}

.modal-body {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── Star Rating ── */
.rating-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.section-label {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
  text-align: center;
}

.stars {
  display: flex;
  gap: 4px;
}

.star-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: var(--color-border);
  transition:
    color 0.1s,
    transform 0.1s;
  border-radius: 6px;
}

.star-btn.active {
  color: var(--color-gold);
}

.star-btn:hover {
  transform: scale(1.15);
}

.rating-hint {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-gold);
  margin: 0;
  height: 1.2em;
  text-align: center;
}

/* ── Form ── */
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-field label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.form-field textarea {
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.15s;
  font-family: inherit;
  color: var(--color-text);
  background: var(--color-surface);
  resize: vertical;
}

.form-field textarea:focus {
  border-color: var(--color-gold);
}

.char-hint {
  font-size: 0.76rem;
  color: var(--color-muted);
  margin: 0;
}

.char-hint.warn {
  color: var(--color-amber);
}

.char-hint.ok {
  color: var(--color-green);
}

.error-msg {
  font-size: 0.85rem;
  color: var(--color-error);
  margin: 0;
  padding: 8px 12px;
  background: var(--color-error-light);
  border-radius: 8px;
}

/* ── Footer ── */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px;
  border-top: 1px solid var(--color-border);
}

.btn-primary {
  background: var(--color-gold);
  color: #fff;
  border: none;
  padding: 9px 20px;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition: filter 0.15s;
}

.btn-primary:hover:not(:disabled) {
  filter: brightness(1.08);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: none;
  color: var(--color-text);
  border: 1.5px solid var(--color-border);
  padding: 9px 18px;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-secondary:hover {
  background: var(--color-surface);
}
</style>
