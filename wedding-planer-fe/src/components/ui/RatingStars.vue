<script setup lang="ts">
defineProps<{ rating: number; count?: number; size?: "sm" | "md" }>();
</script>

<template>
  <div class="rating-stars" :class="size ?? 'md'">
    <span
      v-for="n in 5"
      :key="n"
      class="star"
      :class="{ filled: n <= Math.round(rating) }"
      >★</span
    >
    <span v-if="count !== undefined" class="count">({{ count }})</span>
  </div>
</template>

<style scoped>
.rating-stars {
  display: flex;
  align-items: center;
  gap: 2px;
}
.star {
  color: var(--color-border);
}
.star.filled {
  color: var(--color-gold);
}
.md .star {
  font-size: 1.1rem;
}
.sm .star {
  font-size: 0.85rem;
}
.count {
  font-size: 0.78rem;
  color: var(--color-muted);
  margin-left: 4px;
}
</style>
