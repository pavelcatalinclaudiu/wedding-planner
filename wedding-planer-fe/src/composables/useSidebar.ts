import { ref } from "vue";

// Module-level singleton — shared between Topbar and sidebar components
const sidebarOpen = ref(false);

export function useSidebar() {
  function open() {
    sidebarOpen.value = true;
  }
  function close() {
    sidebarOpen.value = false;
  }
  function toggle() {
    sidebarOpen.value = !sidebarOpen.value;
  }
  return { sidebarOpen, open, close, toggle };
}
