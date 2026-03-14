import { onMounted, onUnmounted, type Ref } from "vue";
import { onBeforeRouteLeave } from "vue-router";

export interface UnsavedChangesOptions {
  /**
   * Message shown in the browser's native "are you sure?" dialog.
   * Note: modern browsers ignore the custom message in `beforeunload` and
   * show their own generic text instead, but the prompt will still appear.
   */
  message?: string;
  /**
   * When true the router guard uses `window.confirm`. Set to false if you
   * want to handle prompt yourself via the returned `requestConfirm` helper.
   */
  useNativeConfirm?: boolean;
}

/**
 * Guards against accidental data loss by:
 *  1. Blocking the browser tab close / refresh via `beforeunload`.
 *  2. Blocking in-app navigation via `onBeforeRouteLeave`.
 *
 * @param isDirty – reactive boolean that is `true` when there are unsaved changes.
 *
 * @example
 * const isDirty = ref(false);
 * watch(form, () => { isDirty.value = true; }, { deep: true });
 * useUnsavedChanges(isDirty);
 */
export function useUnsavedChanges(
  isDirty: Ref<boolean>,
  options: UnsavedChangesOptions = {},
): void {
  const {
    message = "You have unsaved changes. Leave this page?",
    useNativeConfirm = true,
  } = options;

  // ---- 1. Browser refresh / close -----------------------------------------
  function handleBeforeUnload(e: BeforeUnloadEvent): void {
    if (!isDirty.value) return;
    e.preventDefault();
    // Legacy support
    e.returnValue = message;
  }

  onMounted(() => window.addEventListener("beforeunload", handleBeforeUnload));
  onUnmounted(() =>
    window.removeEventListener("beforeunload", handleBeforeUnload),
  );

  // ---- 2. Vue Router navigation -------------------------------------------
  onBeforeRouteLeave((_to, _from, next) => {
    if (!isDirty.value) {
      next();
      return;
    }
    if (useNativeConfirm) {
      if (window.confirm(message)) {
        isDirty.value = false;
        next();
      } else {
        next(false);
      }
    } else {
      // Consumer is expected to handle confirmation elsewhere.
      // Allow navigation by default if useNativeConfirm is off.
      next();
    }
  });
}
