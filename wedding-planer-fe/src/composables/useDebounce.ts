import { ref, watch } from "vue";

export function useDebounce<T>(value: () => T, delay = 300) {
  const debounced = ref<T>(value());
  let timer: ReturnType<typeof setTimeout>;

  watch(value, (newVal) => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      debounced.value = newVal as unknown as T;
    }, delay);
  });

  return debounced;
}
