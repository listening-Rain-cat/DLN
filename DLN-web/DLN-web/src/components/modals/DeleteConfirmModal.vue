<script setup lang="ts">
defineProps<{
  modal: {
    busy: boolean
  }
  title: string
  copy: string
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'confirm'): void
}>()
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-card">
      <div class="modal-heading">
        <p class="eyebrow">确认操作</p>
        <h3>{{ title }}</h3>
      </div>

      <p class="modal-copy">{{ copy }}</p>

      <div class="modal-actions">
        <button type="button" class="soft-button" :disabled="modal.busy" @click="$emit('close')">取消</button>
        <button type="button" class="soft-button modal-danger" :disabled="modal.busy" @click="$emit('confirm')">
          {{ modal.busy ? '删除中...' : '删除' }}
        </button>
      </div>
    </div>
  </div>
</template>
