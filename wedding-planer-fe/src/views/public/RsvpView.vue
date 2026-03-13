<script setup lang="ts">
import { ref } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { coupleApi } from "@/api/couple.api";
import { Sparkles, Mail } from "lucide-vue-next";

const route = useRoute();
const { t } = useI18n();

const step = ref<"form" | "thanks" | "declined">("form");
const loading = ref(false);
const error = ref("");
const form = ref({
  firstName: "",
  lastName: "",
  email: "",
  attending: true,
  plusOne: false,
  plusOneName: "",
  dietary: "NONE",
  message: "",
});

async function submit() {
  if (!form.value.firstName.trim()) {
    error.value = t("rsvp.pleaseName");
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    await coupleApi.submitPublicRsvp(route.params.token as string, {
      firstName: form.value.firstName,
      lastName: form.value.lastName,
      email: form.value.email,
      attending: form.value.attending ? "CONFIRMED" : "DECLINED",
      plusOne: form.value.plusOne,
      plusOneName: form.value.plusOneName,
      dietary: form.value.dietary,
      message: form.value.message,
    });
    step.value = form.value.attending ? "thanks" : "declined";
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? t("errors.serverError");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="rsvp-page">
    <div class="rsvp-card">
      <p class="rsvp-logo">Eternelle ♡</p>
      <h1>RSVP</h1>

      <div v-if="step === 'form'" class="rsvp-form">
        <p class="rsvp-subtitle">
          {{ t("rsvp.rsvpSubtitle") }}
        </p>

        <div class="field">
          <label>{{ t("rsvp.fullNameLabel") }}</label>
          <div style="display: flex; gap: 10px">
            <input
              v-model="form.firstName"
              class="input"
              placeholder="First name"
              style="flex: 1"
            />
            <input
              v-model="form.lastName"
              class="input"
              placeholder="Last name"
              style="flex: 1"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t("rsvp.email") }}</label>
          <input
            v-model="form.email"
            type="email"
            class="input"
            placeholder="jane@example.com"
          />
        </div>

        <div class="attendance-choice">
          <button
            class="choice-btn"
            :class="{ selected: form.attending }"
            @click="form.attending = true"
          >
            {{ t("rsvp.joyfullyAccepts") }}
          </button>
          <button
            class="choice-btn decline"
            :class="{ selected: !form.attending }"
            @click="form.attending = false"
          >
            {{ t("rsvp.regretfullyDeclines") }}
          </button>
        </div>

        <template v-if="form.attending">
          <div class="field">
            <label>{{ t("rsvp.dietaryLabel") }}</label>
            <select v-model="form.dietary" class="input">
              <option value="NONE">No restrictions</option>
              <option value="VEGAN">Vegan</option>
              <option value="VEGETARIAN">Vegetarian</option>
              <option value="GLUTEN_FREE">Gluten-free</option>
              <option value="HALAL">Halal</option>
              <option value="KOSHER">Kosher</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <label class="check-label">
            <input type="checkbox" v-model="form.plusOne" />
            {{ t("rsvp.plusOneLabel") }}
          </label>
          <div v-if="form.plusOne" class="field">
            <label>+1 Name</label>
            <input
              v-model="form.plusOneName"
              class="input"
              placeholder="Guest's name"
            />
          </div>
        </template>

        <div class="field">
          <label>Message to the couple</label>
          <textarea
            v-model="form.message"
            class="input"
            rows="2"
            placeholder="Congratulations! 🥂"
            style="resize: vertical"
          ></textarea>
        </div>

        <p v-if="error" class="error-msg">{{ error }}</p>
        <button class="submit-btn" :disabled="loading" @click="submit">
          {{ loading ? t("rsvp.sending") : t("rsvp.sendButton") }}
        </button>
      </div>

      <div v-else-if="step === 'thanks'" class="result-state">
        <div class="result-icon"><Sparkles :size="48" /></div>
        <h2>{{ t("rsvp.celebrate") }}</h2>
        <p>{{ t("rsvp.received") }}</p>
      </div>

      <div v-else class="result-state">
        <div class="result-icon"><Mail :size="48" /></div>
        <h2>{{ t("rsvp.missYou") }}</h2>
        <p>
          {{ t("rsvp.thanksForLetting") }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rsvp-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a1a 0%, #2c2c2c 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  font-family: Georgia, serif;
}
.rsvp-card {
  background: #fffdf9;
  border-radius: 20px;
  padding: 48px;
  max-width: 480px;
  width: 100%;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.3);
}
.rsvp-logo {
  margin: 0 0 8px;
  font-size: 1rem;
  color: #b8860b;
  text-align: center;
}
h1 {
  margin: 0 0 6px;
  font-size: 2rem;
  font-weight: 400;
  text-align: center;
  color: #2c2c2c;
}
.rsvp-subtitle {
  text-align: center;
  color: #888;
  font-size: 0.95rem;
  margin: 0 0 28px;
  font-family: sans-serif;
}
.rsvp-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
label {
  font-size: 0.84rem;
  font-weight: 600;
  color: #444;
  font-family: sans-serif;
}
.input {
  padding: 10px 14px;
  border: 1.5px solid #d9d4ca;
  border-radius: 8px;
  font-size: 0.9rem;
  font-family: sans-serif;
  outline: none;
}
.input:focus {
  border-color: #b8860b;
}
.attendance-choice {
  display: flex;
  gap: 10px;
}
.choice-btn {
  flex: 1;
  padding: 12px;
  border: 1.5px solid #d9d4ca;
  border-radius: 8px;
  background: none;
  cursor: pointer;
  font-size: 0.9rem;
  font-family: sans-serif;
  font-weight: 600;
  transition: all 0.15s;
}
.choice-btn.selected {
  border-color: #b8860b;
  background: #fdf8ee;
  color: #b8860b;
}
.choice-btn.decline.selected {
  border-color: #e74c3c;
  background: #fff0f0;
  color: #e74c3c;
}
.check-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-family: sans-serif;
  cursor: pointer;
}
.error-msg {
  color: #e74c3c;
  font-size: 0.85rem;
  font-family: sans-serif;
  margin: 0;
}
.submit-btn {
  background: #b8860b;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 14px;
  font-weight: 700;
  cursor: pointer;
  font-size: 1rem;
  font-family: sans-serif;
}
.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.result-state {
  text-align: center;
  padding: 20px 0;
}
.result-icon {
  font-size: 3rem;
  margin-bottom: 16px;
}
.result-state h2 {
  font-size: 1.4rem;
  font-weight: 400;
  margin: 0 0 10px;
  color: #2c2c2c;
}
.result-state p {
  color: #888;
  font-size: 0.9rem;
  font-family: sans-serif;
  margin: 0;
}
</style>
