import { ref } from "vue";

export interface ConfirmOptions {
  title?: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
}

type Resolver = (value: boolean) => void;

// Module-level singleton — same instance across all imports
const visible = ref(false);
const message = ref("");
const title = ref("Are you sure?");
const confirmText = ref("Confirm");
const cancelText = ref("Cancel");
const danger = ref(true);
let resolver: Resolver | null = null;

export function useConfirm() {
  function ask(msg: string, opts: ConfirmOptions = {}): Promise<boolean> {
    message.value = msg;
    title.value = opts.title ?? "Are you sure?";
    confirmText.value = opts.confirmText ?? "Confirm";
    cancelText.value = opts.cancelText ?? "Cancel";
    danger.value = opts.danger ?? true;
    visible.value = true;
    return new Promise<boolean>((resolve) => {
      resolver = resolve;
    });
  }

  function resolve(value: boolean) {
    visible.value = false;
    resolver?.(value);
    resolver = null;
  }

  return {
    visible,
    message,
    title,
    confirmText,
    cancelText,
    danger,
    ask,
    resolve,
  };
}
