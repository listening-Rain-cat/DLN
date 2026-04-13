<script setup lang="ts">
type KnowledgeBaseModalMode = 'create' | 'edit'

defineProps<{
  modal: {
    mode: KnowledgeBaseModalMode
    name: string
    description: string
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
        <p class="eyebrow">知识库</p>
        <h3>{{ modal.mode === 'create' ? '新建知识库' : '编辑知识库' }}</h3>
      </div>

      <label class="field">
        <span>名称</span>
        <input v-model="modal.name" type="text" placeholder="例如：分布式系统" />
      </label>

      <label class="field">
        <span>描述</span>
        <textarea v-model="modal.description" rows="4" placeholder="给这个知识库写一段简短说明"></textarea>
      </label>

      <div class="modal-actions">
        <button type="button" class="soft-button" @click="$emit('close')">取消</button>
        <button type="button" class="soft-button accent" @click="$emit('submit')">
          {{ modal.mode === 'create' ? '创建' : '保存' }}
        </button>
      </div>
    </div>
  </div>
</template>
