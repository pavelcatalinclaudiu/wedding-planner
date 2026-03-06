<script setup lang="ts">
import { ref, reactive } from "vue";
import { offersApi } from "@/api/offers.api";

const props = defineProps<{ leadId: string }>();
const emit = defineEmits<{ (e: "close"): void; (e: "sent"): void }>();

const saving = ref(false);
const error = ref("");

const form = reactive<{
  packageName: string;
  totalPrice: number | "";
  depositAmount: number | "";
  expiryDays: number;
  items: Array<{ name: string; quantity: number; unitPrice: number | "" }>;
}>({
  packageName: "",
  totalPrice: "",
  depositAmount: "",
  expiryDays: 7,
  items: [{ name: "", quantity: 1, unitPrice: "" }],
});

function addItem() {
  form.items.push({ name: "", quantity: 1, unitPrice: "" });
}

function removeItem(i: number) {
  if (form.items.length > 1) form.items.splice(i, 1);
}

function computedTotal() {
  return form.items.reduce((sum, item) => {
    const p = Number(item.unitPrice) || 0;
    return sum + p * (item.quantity || 1);
  }, 0);
}

function syncTotal() {
  const t = computedTotal();
  if (t > 0) form.totalPrice = t;
}

async function submit() {
  error.value = "";
  if (!form.packageName.trim()) {
    error.value = "Package name is required.";
    return;
  }
  if (!form.totalPrice || Number(form.totalPrice) <= 0) {
    error.value = "Total price must be greater than 0.";
    return;
  }

  const expiresAt = new Date(
    Date.now() + (form.expiryDays || 7) * 86_400_000,
  ).toISOString();

  saving.value = true;
  try {
    const packageDetails = [
      form.packageName.trim(),
      ...form.items
        .filter((i) => i.name.trim())
        .map((i) => `- ${i.name}: ${i.quantity}x £${i.unitPrice}`),
    ].join("\n");
    await offersApi.create({
      leadId: props.leadId,
      packageDetails,
      price: Number(form.totalPrice),
      expiresAt,
    });
    emit("sent");
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? "Failed to send offer.";
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>Send an Offer</h3>
        <button class="close-btn" @click="emit('close')">✕</button>
      </div>

      <div class="modal-body">
        <p v-if="error" class="form-error">{{ error }}</p>

        <!-- Package name -->
        <div class="field">
          <label>Package Name *</label>
          <input
            v-model="form.packageName"
            placeholder="e.g. Photography Full Day"
          />
        </div>

        <!-- Line items -->
        <div class="field">
          <label>Items</label>
          <div v-for="(item, i) in form.items" :key="i" class="item-row">
            <input
              v-model="item.name"
              class="item-name"
              placeholder="Item name"
              @blur="syncTotal"
            />
            <input
              v-model.number="item.quantity"
              type="number"
              min="1"
              class="item-qty"
              placeholder="Qty"
              @change="syncTotal"
            />
            <input
              v-model.number="item.unitPrice"
              type="number"
              min="0"
              class="item-price"
              placeholder="Unit price"
              @input="syncTotal"
            />
            <button
              class="remove-item"
              :disabled="form.items.length === 1"
              @click="removeItem(i)"
            >
              −
            </button>
          </div>
          <button class="add-item-btn" @click="addItem">+ Add item</button>
        </div>

        <!-- Total -->
        <div class="field two-col">
          <div>
            <label>Total Price *</label>
            <input
              v-model.number="form.totalPrice"
              type="number"
              min="0"
              placeholder="0.00"
            />
          </div>
          <div>
            <label>Deposit Amount</label>
            <input
              v-model.number="form.depositAmount"
              type="number"
              min="0"
              placeholder="Optional"
            />
          </div>
        </div>

        <!-- Expiry -->
        <div class="field">
          <label>Offer valid for (days)</label>
          <input
            v-model.number="form.expiryDays"
            type="number"
            min="1"
            max="90"
          />
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-cancel" @click="emit('close')">Cancel</button>
        <button class="btn-send" :disabled="saving" @click="submit">
          {{ saving ? "Sending…" : "Send Offer" }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: var(--color-white);
  border-radius: 16px;
  width: 100%;
  max-width: 500px;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  overflow: hidden;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid var(--color-border);
}
.modal-header h3 {
  margin: 0;
  font-size: 1.05rem;
}
.close-btn {
  background: none;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  color: var(--color-muted);
  padding: 0 4px;
}
.modal-body {
  padding: 18px 22px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.form-error {
  background: var(--chip-red-bg);
  color: var(--color-error);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 0.85rem;
  margin: 0;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.field label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-muted);
}
.field input {
  padding: 9px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
}
.field input:focus {
  border-color: var(--color-gold);
}

.two-col {
  flex-direction: row;
  gap: 12px;
}
.two-col > div {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.item-row {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-bottom: 6px;
}
.item-name {
  flex: 3;
}
.item-qty {
  flex: 1;
  min-width: 50px;
}
.item-price {
  flex: 2;
}
.remove-item {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  width: 28px;
  height: 28px;
  cursor: pointer;
  font-size: 1rem;
  color: var(--color-muted);
  flex-shrink: 0;
}
.remove-item:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.add-item-btn {
  background: none;
  border: none;
  color: var(--color-gold);
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  text-align: left;
}

.modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 16px 22px;
  border-top: 1px solid var(--color-border);
}
.btn-cancel {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 9px 18px;
  cursor: pointer;
  font-size: 0.88rem;
}
.btn-send {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 9px 22px;
  font-weight: 700;
  font-size: 0.88rem;
  cursor: pointer;
}
.btn-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
