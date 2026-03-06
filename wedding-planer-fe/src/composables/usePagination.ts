import { ref, computed } from "vue";

export function usePagination(initialPage = 1, initialPageSize = 20) {
  const page = ref(initialPage);
  const pageSize = ref(initialPageSize);
  const total = ref(0);

  const totalPages = computed(() => Math.ceil(total.value / pageSize.value));
  const hasNextPage = computed(() => page.value < totalPages.value);
  const hasPrevPage = computed(() => page.value > 1);

  function nextPage() {
    if (hasNextPage.value) page.value++;
  }

  function prevPage() {
    if (hasPrevPage.value) page.value--;
  }

  function goTo(p: number) {
    if (p >= 1 && p <= totalPages.value) page.value = p;
  }

  function setTotal(n: number) {
    total.value = n;
  }

  function reset() {
    page.value = initialPage;
  }

  return {
    page,
    pageSize,
    total,
    totalPages,
    hasNextPage,
    hasPrevPage,
    nextPage,
    prevPage,
    goTo,
    setTotal,
    reset,
  };
}
