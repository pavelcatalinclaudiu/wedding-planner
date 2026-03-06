/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_WS_BASE_URL: string;
  readonly VITE_DAILY_CO_SCRIPT: string;
  readonly VITE_APP_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// Daily.co typings (loaded via CDN script)
interface Window {
  DailyIframe: {
    createFrame: (
      container: HTMLElement | null,
      options?: Record<string, unknown>,
    ) => DailyCallObject;
  };
}

interface DailyCallObject {
  join(options: { url: string; token?: string }): Promise<void>;
  leave(): Promise<void>;
  destroy(): void;
  on(event: string, callback: (...args: unknown[]) => void): DailyCallObject;
  off(event: string, callback: (...args: unknown[]) => void): DailyCallObject;
}
