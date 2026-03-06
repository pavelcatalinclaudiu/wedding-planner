import { ref, watchEffect, type Ref } from "vue";
import {
  differenceInDays,
  differenceInHours,
  differenceInMinutes,
} from "date-fns";

export function useCountdown(weddingDate: Ref<string | undefined> | undefined) {
  const days = ref(0);
  const hours = ref(0);
  const minutes = ref(0);
  const isPast = ref(false);

  watchEffect(() => {
    if (!weddingDate?.value) return;
    const target = new Date(weddingDate.value);
    const now = new Date();
    isPast.value = target <= now;
    days.value = Math.max(0, differenceInDays(target, now));
    hours.value = Math.max(0, differenceInHours(target, now) % 24);
    minutes.value = Math.max(0, differenceInMinutes(target, now) % 60);
  });

  return { days, hours, minutes, isPast };
}
