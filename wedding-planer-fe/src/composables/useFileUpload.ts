import { ref } from "vue";
import apiClient from "@/api/client";

export interface UploadProgress {
  name: string;
  progress: number;
  done: boolean;
  error?: string;
  url?: string;
}

export function useFileUpload() {
  const uploads = ref<UploadProgress[]>([]);
  const uploading = ref(false);
  const progress = ref(0);

  async function upload(
    endpoint: string,
    files: File | File[],
    extraFields?: Record<string, string>,
  ): Promise<string[]> {
    const fileList = Array.isArray(files) ? files : [files];
    const results: string[] = [];

    uploading.value = true;
    progress.value = 0;
    for (const file of fileList) {
      const entry: UploadProgress = {
        name: file.name,
        progress: 0,
        done: false,
      };
      uploads.value.push(entry);

      const form = new FormData();
      form.append("file", file);
      if (extraFields) {
        Object.entries(extraFields).forEach(([k, v]) => form.append(k, v));
      }

      try {
        const res = await apiClient.post<{ url: string }>(endpoint, form, {
          headers: { "Content-Type": "multipart/form-data" },
          onUploadProgress: (e) => {
            entry.progress = Math.round((e.loaded / (e.total ?? 1)) * 100);
            progress.value = entry.progress;
          },
        });
        entry.progress = 100;
        entry.done = true;
        entry.url = res.data.url;
        results.push(res.data.url);
      } catch (err: unknown) {
        entry.error = (err as Error).message ?? "Upload failed";
      }
    }
    uploading.value = false;

    return results;
  }

  function clearUploads() {
    uploads.value = [];
    progress.value = 0;
    uploading.value = false;
  }

  return { uploads, upload, clearUploads, uploading, progress };
}
