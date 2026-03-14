import { onMounted, onUnmounted } from "vue";

export type ShortcutMap = Record<string, (e: KeyboardEvent) => void>;

export interface KeyboardShortcutsOptions {
  /**
   * When true (default), shortcuts are ignored while the user is typing
   * inside an input, textarea, select, or contenteditable element.
   */
  ignoreWhenTyping?: boolean;
  /**
   * Only fire when the modifier combination matches.
   * Use `ctrl`, `meta`, `alt`, `shift`.
   */
  modifiers?: {
    ctrl?: boolean;
    meta?: boolean;
    alt?: boolean;
    shift?: boolean;
  };
}

/**
 * Registers keyboard shortcuts that are automatically cleaned up when the
 * component is unmounted.
 *
 * @example
 * useKeyboardShortcuts({
 *   'n': () => openNewGuestModal(),
 *   'f': () => searchInput.value?.focus(),
 *   'Escape': () => closeModal(),
 * });
 *
 * // With modifier (Ctrl/Cmd + S):
 * useKeyboardShortcuts(
 *   { 's': () => save() },
 *   { modifiers: { ctrl: true } }
 * );
 */
export function useKeyboardShortcuts(
  shortcuts: ShortcutMap,
  options: KeyboardShortcutsOptions = {},
): void {
  const { ignoreWhenTyping = true, modifiers } = options;

  function handler(e: KeyboardEvent): void {
    // Optionally skip shortcuts while typing
    if (ignoreWhenTyping) {
      const tag = (e.target as HTMLElement).tagName;
      if (["INPUT", "TEXTAREA", "SELECT"].includes(tag)) return;
      if ((e.target as HTMLElement).isContentEditable) return;
    }

    // Check modifier requirements
    if (modifiers) {
      if (modifiers.ctrl !== undefined && e.ctrlKey !== modifiers.ctrl) return;
      if (modifiers.meta !== undefined && e.metaKey !== modifiers.meta) return;
      if (modifiers.alt !== undefined && e.altKey !== modifiers.alt) return;
      if (modifiers.shift !== undefined && e.shiftKey !== modifiers.shift)
        return;
    }

    const cb = shortcuts[e.key];
    if (cb) {
      cb(e);
    }
  }

  onMounted(() => document.addEventListener("keydown", handler));
  onUnmounted(() => document.removeEventListener("keydown", handler));
}
