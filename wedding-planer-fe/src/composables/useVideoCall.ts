import { ref } from "vue";

declare global {
  interface Window {
    JitsiMeetExternalAPI: new (
      domain: string,
      options: Record<string, unknown>,
    ) => JitsiApi;
  }
}

interface JitsiApi {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  addEventListeners(listeners: Record<string, (event?: any) => void>): void;
  executeCommand(command: string): void;
  dispose(): void;
}

/**
 * Parse a JaaS or public Jitsi URL.
 * JaaS:   https://8x8.vc/<appId>/<room>  → domain=8x8.vc, roomName=<appId>/<room>
 * Public: https://meet.jit.si/<room>     → domain=meet.jit.si, roomName=<room>
 */
function parseJitsiUrl(roomUrl: string): {
  domain: string;
  roomName: string;
  scriptSrc: string;
} {
  const jaas = roomUrl.match(/^https:\/\/8x8\.vc\/(([^/]+)\/(.+))$/);
  if (jaas) {
    const appId = jaas[2];
    return {
      domain: "8x8.vc",
      roomName: jaas[1], // <appId>/<room>
      scriptSrc: `https://8x8.vc/${appId}/external_api.js`, // JaaS-specific script
    };
  }
  return {
    domain: "meet.jit.si",
    roomName: roomUrl.replace("https://meet.jit.si/", ""),
    scriptSrc: "https://meet.jit.si/external_api.js",
  };
}

function loadScript(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(
      "script[data-jitsi-api]",
    );
    if (existing) {
      if (existing.src === src) {
        resolve();
        return;
      }
      existing.remove();
      // @ts-ignore
      delete window.JitsiMeetExternalAPI;
    }
    const s = document.createElement("script");
    s.src = src;
    s.setAttribute("data-jitsi-api", "");
    s.onload = () => resolve();
    s.onerror = () => reject(new Error(`Failed to load ${src}`));
    document.head.appendChild(s);
  });
}

/**
 * Ask the browser for camera + mic access before Jitsi's iframe tries to,
 * so the permission state is already "granted" when the embedded frame requests it.
 * The tracks are immediately stopped — Jitsi will open its own stream.
 */
async function primeMediaPermissions(): Promise<void> {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: true,
    });
    stream.getTracks().forEach((t) => t.stop());
  } catch {
    // Permission denied or no camera — Jitsi will handle it gracefully
  }
}

export function useVideoCall() {
  const jitsiApi = ref<JitsiApi | null>(null);
  const callActive = ref(false);
  const jitsiError = ref("");

  async function join(roomUrl: string, token?: string | null) {
    jitsiError.value = "";

    const container = document.getElementById("jitsi-call-container");
    if (!container)
      throw new Error("Video container #jitsi-call-container not found in DOM");

    // Prime browser permissions so the iframe inherits "granted" state
    await primeMediaPermissions();

    const { domain, roomName, scriptSrc } = parseJitsiUrl(roomUrl);

    await loadScript(scriptSrc);

    const options: Record<string, unknown> = {
      roomName,
      parentNode: container,
      width: "100%",
      height: "100%",
      configOverwrite: {
        prejoinPageEnabled: false,
        prejoinConfig: { enabled: false },
        startWithAudioMuted: false,
        startWithVideoMuted: false,
        disableDeepLinking: true,
        // Disable P2P so both participants always use the JVB bridge.
        // Without this, remote tracks arriving on the P2P channel are
        // silently dropped when the conference is in JVB mode, causing
        // the "I can see myself but not the other person" problem.
        p2p: { enabled: false },
        // Disable E2EE to suppress a "Cannot read properties of null (reading 'source')"
        // crash inside supportsInsertableStreams / BrowserCapabilities that fires on
        // conference join in Chrome when the browser's MediaStreamTrack.source is null.
        // Use both keys: nested form for newer JaaS bundles, flat key for older ones.
        e2ee: { enabled: false },
        disableE2EE: true,
        // Use configOverwrite.toolbarButtons (supported by all Jitsi versions
        // including JaaS). The old interfaceConfigOverwrite.TOOLBAR_BUTTONS
        // is deprecated and can prevent the toolbar from rendering at all.
        toolbarButtons: ["microphone", "camera", "hangup", "chat", "tileview"],
        // Explicitly request HD video so the browser surfaces a camera prompt
        constraints: {
          video: { height: { ideal: 720, max: 1080, min: 240 } },
        },
      },
      interfaceConfigOverwrite: {
        SHOW_JITSI_WATERMARK: false,
      },
    };

    if (token) options.jwt = token;

    return new Promise<void>((resolve, reject) => {
      try {
        jitsiApi.value = new window.JitsiMeetExternalAPI(domain, options);
      } catch (e: any) {
        reject(new Error(e?.message ?? "Failed to initialize Jitsi"));
        return;
      }

      // Resolve as soon as Jitsi signals the conference is ready
      jitsiApi.value.addEventListeners({
        videoConferenceJoined: () => {
          callActive.value = true;
          resolve();
        },
        readyToClose: handleCallEnd,
        errorOccurred: (event: any) => {
          const msg =
            event?.error?.message ??
            event?.type ??
            "Jitsi reported an error — check camera/microphone permissions";
          jitsiError.value = msg;
          reject(new Error(msg));
        },
      });

      // Safety timeout: if videoConferenceJoined never fires, still resolve
      // so the connecting spinner is removed and the Jitsi iframe is visible.
      setTimeout(() => resolve(), 15_000);
    });
  }

  function handleCallEnd() {
    callActive.value = false;
    jitsiApi.value?.dispose();
    jitsiApi.value = null;
  }

  function leave() {
    jitsiApi.value?.executeCommand("hangup");
    handleCallEnd();
  }

  return { join, leave, callActive, jitsiError };
}
