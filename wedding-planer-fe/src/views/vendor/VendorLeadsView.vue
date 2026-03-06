<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
onMounted(() => router.replace("/vendor/leads"));
</script>

<template>
  <div />
</template>
