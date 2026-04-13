<script setup lang="ts">
defineProps<{
  modal: {
    name: string
    busy: boolean
  }
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'submit'): void
}>()
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-card">
      <div class="modal-heading">
        <p class="eyebrow">文件夹</p>
        <h3>重命名文件夹</h3>
      </div>

      <label class="field">
        <span>文件夹名称</span>
        <input v-model="modal.name" type="text" placeholder="例如：阅读清单" />
      </label>

      <div class="modal-actions">
        <button type="button" class="soft-button" :disabled="modal.busy" @click="$emit('close')">取消</button>
        <button type="button" class="soft-button accent" :disabled="modal.busy" @click="$emit('submit')">
          {{ modal.busy ? '保存中...' : '保存名称' }}
        </button>
      </div>
    </div>
  </div>
</template>
